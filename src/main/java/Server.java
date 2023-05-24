import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
       private static final int DEFAULT_THRED_POOL_SIZE = 64;

    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private static final int DEFAULT_PORT = 9999;


    public Server(int poolSize) {
     }

    public void run(int port) {
        try (final var serverSocket = new ServerSocket(DEFAULT_PORT)) {
            while (true) {
                try {
                    final var socket = serverSocket.accept();
                    ThreadPoolExecutor executor = new ThreadPoolExecutor();// что указывать в параметрах?
                                 executor.execute(() -> connectionProcessingSocket(new Socket()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectionProcessingSocket(Socket socket) {
        //  try  {
        while (true) {
            try (socket;
                 final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final var out = new BufferedOutputStream(socket.getOutputStream());
            ) {
                // read only request line for simplicity строка запроса только для чтения для простоты
                // must be in form GET /path HTTP/1.1 должно быть в форме GET /path HTTP/1.1
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    // just close socket
                    continue;

                }

                final var path = parts[1];
                if (!validPaths.contains(path)) {
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                    continue;
                }

                final var filePath = Path.of(".", "public", path);
                final var mimeType = Files.probeContentType(filePath);

                // special case for classic специальный чехол для классического
                if (path.equals("/classic.html")) {
                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                    continue;
                }

                final var length = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();
//                    }
//                }
                //             }
                //     }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


