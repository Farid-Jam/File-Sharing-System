package fileSharingSystem;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;

public class Server {
    private static final int PORT = 1234;  // Port on which the server listens for client connections
    private static Path sharedFolder;  // Path to the shared folder for file operations

    public static void main(String[] args) throws IOException {
        // Ensure the server is provided with the shared folder path
        if (args.length != 1) {
            System.err.println("Usage: java Server <shared_folder>");
            System.exit(1);
        }

        sharedFolder = Paths.get(args[0]);
        if (!Files.isDirectory(sharedFolder)) {
            System.err.println("Shared folder must be a directory.");
            System.exit(1);
        }

        // Start the server socket to listen for incoming client connections
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                // Accept incoming client connection and start a new thread to handle it
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientConnectionHandler(clientSocket)).start();
            }
        }
    }

    // Implement runnable for threading behaviour
    private static class ClientConnectionHandler implements Runnable {
        private final Socket socket;  // The socket for the current client connection

        public ClientConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (InputStream input = socket.getInputStream();
                 OutputStream output = socket.getOutputStream()) {

                // Read the command sent by the client
                ByteArrayOutputStream commandBuffer = new ByteArrayOutputStream();
                int b;
                while ((b = input.read()) != -1 && b != '\n') {
                    commandBuffer.write(b);
                }
                String commandLine = commandBuffer.toString("UTF-8").trim();
                String[] parts = commandLine.split(" ", 2);
                String command = parts[0];
                String filename = parts.length > 1 ? parts[1] : "";

                // Handle the received command
                switch (command) {
                    case "DIR" -> handleDir(output);  // List files in the shared folder
                    case "UPLOAD" -> handleUpload(input, filename);  // Handle file upload
                    case "DOWNLOAD" -> handleDownload(output, filename);  // Handle file download
                    default -> System.err.println("Unknown command: " + command);
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    socket.close();  // Close the client socket after processing
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }

        // Handle 'DIR' command to list files in the shared folder
        private void handleDir(OutputStream output) throws IOException {
            StringBuilder response = new StringBuilder();
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(sharedFolder)) {
                // Append each filename to the response
                for (Path file : dirStream) {
                    response.append(file.getFileName()).append("\n");
                }
            }
            output.write(response.toString().getBytes("UTF-8"));
        }

        // Handle 'UPLOAD' command to upload a file from the client
        private void handleUpload(InputStream input, String filename) throws IOException {
            Path filePath = sharedFolder.resolve(filename);  // Path to the uploaded file
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);  // Save the uploaded file

            // Show a success message after the file upload is complete
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    filename + " has been uploaded to the server.", "File Uploaded", JOptionPane.INFORMATION_MESSAGE));
        }

        // Handle 'DOWNLOAD' command to send a file to the client
        private void handleDownload(OutputStream output, String filename) throws IOException {
            Path filePath = sharedFolder.resolve(filename);  // Path to the requested file
            if (Files.exists(filePath)) {
                Files.copy(filePath, output);  // Send the file content to the client

                // Show a success message after the file download is complete
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        filename + " has been downloaded from the server.", "File Downloaded", JOptionPane.INFORMATION_MESSAGE));
            } else {
                // Show an error message if the file does not exist
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "File not found: " + filename, "Error", JOptionPane.ERROR_MESSAGE));
            }
        }
    }
}
