/*
    This file is part of Nephren.

    HttpDownload.java
    Copyright (C) 2020, 2021  Relius Wang

    Nephren is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Nephren is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Nephren.  If not, see <https://www.gnu.org/licenses/>.
 */

package util;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestException;
import java.time.Duration;
import java.util.Observable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class HttpDownload extends Observable implements Callable<URI> {
    // Buffer 最大 64 KiB
    private static final AtomicInteger MAX_BUFFER_SIZE =
            new AtomicInteger(65536);
    // 無上限的執行緒池
    private static final ExecutorService executorService =
            Executors.newCachedThreadPool();
    // 建立 HttpClient，預設使用 HTTP2
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .priority(1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .proxy(ProxySelector.getDefault())
            .build();
    private final URI uri;
    private final String hash;
    private final String hashAlgor;
    private final AtomicLong size = new AtomicLong();
    private final AtomicLong downloaded = new AtomicLong();
    private Statuses status;

    static {
        try {
            if (!Files.exists(Paths.get("Download/"))) {
                Files.createDirectory(Paths.get("Download/"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HttpDownload(URI uri, String hashAlgor, String hash) {
        this.uri = uri;
        this.hashAlgor = hashAlgor;
        this.hash = hash;
        size.set(-1L);
        downloaded.set(0);
        status = Statuses.DOWNLOADING;
        download();
    }

    public String getUri() {
        return uri.toString();
    }

    public long getSize() {
        return size.get();
    }

    public double getProgress() {
        return (downloaded.doubleValue() / size.doubleValue()) * 100;
    }

    public Statuses getStatus() {
        return status;
    }

    // 取得 URI 裡的檔案名稱
    private String getFileName(@NotNull URI uri) {
        return "Download/" + Paths.get(uri.getPath()).getFileName().toString();
    }

    // 建立 HttpRequest
    private HttpRequest getHttpRequest(@NotNull URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .version(HttpClient.Version.HTTP_2)
                // 設定下載範圍
                .header("Range", "bytes=" + downloaded.get() + "-")
                .build();
    }

    private void stateChanged() {
        setChanged();
        notifyObservers();
    }

    public void onPause() {
        status = Statuses.PAUSED;
        stateChanged();
    }

    public void onResume() {
        status = Statuses.DOWNLOADING;
        stateChanged();
        download();
    }

    public void onCancel() {
        status = Statuses.CANCELLED;
        stateChanged();
    }

    public void onError() {
        status = Statuses.ERROR;
        stateChanged();
    }

    public void onComplete() {
        status = Statuses.COMPLETE;
        stateChanged();
    }

    // 本類別包裝成 FutureTask，並送到執行緒池執行
    private void download() {
        FutureTask<URI> future = new FutureTask<>(this);
        executorService.execute(future);
    }

    @Override
    public URI call() {
        // 設定請求物件
        HttpRequest request = getHttpRequest(uri);
        // 回傳物件包含 Header 和 Body(InputStream)
        HttpResponse<InputStream> response =
                client.sendAsync(request, BodyHandlers.ofInputStream()).join();

        // 確認回應代碼在 200 範圍
        if (response.statusCode() / 100 != 2) {
            onError();
        }
        // 取得下載內容大小
        AtomicLong contentLength = new AtomicLong(response.headers()
                .firstValueAsLong("Content-Length").getAsLong());

        // 設定 Size 屬性為下載內容大小
        if (contentLength.get() >= 1) {
            size.compareAndSet(-1L, contentLength.get());
            stateChanged();
        } else {
            onError();
        }
        // 用 BufferedInputStream 包裝 InputStream，減少碎片寫入
        // 並開啟檔案
        try (BufferedInputStream bis = new BufferedInputStream(response.body());
             RandomAccessFile raf = new RandomAccessFile(getFileName(uri), "rw")) {
            // 指派到檔案下載的最後位置
            raf.seek(downloaded.get());

            byte[] Buffer;

            while (status == Statuses.DOWNLOADING) {
                // 設定 byte[] 容量，最大為 MAX_BUFFER_SIZE
                if (size.get() - downloaded.get() > MAX_BUFFER_SIZE.get()) {
                    Buffer = new byte[MAX_BUFFER_SIZE.get()];
                } else {
                    Buffer = new byte[(int) (size.get() - downloaded.get())];
                }
                // 讀出 Buffer 並送到 Buffer 中
                AtomicInteger read = new AtomicInteger(bis.read(Buffer));

                // 讀完 InputStream 則跳出 While 迴圈
                if (read.get() <= 0) {
                    break;
                }
                // 將 Buffer 寫入檔案
                raf.write(Buffer, 0, read.get());
                downloaded.addAndGet(read.get());
                stateChanged();
            }
            // 下載完成則改變狀態成 Complete
            if (status == Statuses.DOWNLOADING) {
                onComplete();
                // 如果有 Hash 值就校驗，並決定是否重載
                if (!hash.equals("")) {
                    ByteBuffer endByteBuffer = ByteBuffer.wrap(
                            Files.readAllBytes(Paths.get(getFileName(uri))));
                    if (!compareHash(endByteBuffer, hashAlgor, hash)) {
                        Files.deleteIfExists(Paths.get(getFileName(uri)));
                        downloaded.set(0);
                        onResume();
                    }
                }
            }
        } catch (IOException ex) {
            onError();
            ex.printStackTrace();
        }
        return uri;
    }

    private boolean compareHash(ByteBuffer file, @NotNull String hashAlgor, @NotNull String hash) {
        String fileHash = "";
        try {
            fileHash = HashUtil.apply(file, hashAlgor);
            System.out.println(fileHash);
        } catch (DigestException ex) {
            ex.printStackTrace();
        }
        return hash.equalsIgnoreCase(fileHash);
    }

    // 下載狀態列表
    public enum Statuses {
        DOWNLOADING, PAUSED, COMPLETE, CANCELLED, ERROR
    }
}
