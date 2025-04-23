package fileSharingSystem;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.swing.*;

public class Client extends JFrame {
    private final String serverHost;  // Server host address
    private Path localFolder;  // Local folder for file storage
    private final DefaultListModel<String> localListModel = new DefaultListModel<>();
    private final DefaultListModel<String> serverListModel = new DefaultListModel<>();
    private JList<String> localList;  // JList to display local files
    private JList<String> serverList;  // JList to display server files

    // Constructor to initialize client with server host and local folder path
    public Client(String serverHost, String localFolderPath) {
        this.serverHost = serverHost;
        this.localFolder = Paths.get(localFolderPath);

        // Check if the provided local folder exists
        if (!Files.isDirectory(localFolder)) {
            JOptionPane.showMessageDialog(null, "Local folder must be a directory.");
            System.exit(1);
        }

        initializeUI();  // Initialize UI components
        refreshLocalFiles();  // Load local files
        refreshServerFiles();  // Load server files
    }

    // Initialize UI components
    private void initializeUI() {
        setTitle("File Sharer");  // Set window title
        setSize(800, 600);  // Set window size
        setDefaultCloseOperation(EXIT_ON_CLOSE);  // Close the application when the window is closed

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();  // Panel to hold buttons
        JButton uploadButton = new JButton("Upload");
        JButton downloadButton = new JButton("Download");
        JButton configureButton = new JButton("Change Shared Folder");

        // Add buttons to the button panel
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(configureButton);

        // Panels for client (local) and server file displays
        JPanel clientPanel = new JPanel(new BorderLayout());
        JPanel serverPanel = new JPanel(new BorderLayout());

        localList = new JList<>(localListModel);  // Create JList for local files
        serverList = new JList<>(serverListModel);  // Create JList for server files

        // Labels for local and server files
        JLabel clientLabel = new JLabel("Your Files", JLabel.CENTER);
        JLabel serverLabel = new JLabel("Server Files", JLabel.CENTER);

        // Add components to client and server panels
        clientPanel.add(clientLabel, BorderLayout.NORTH);
        clientPanel.add(new JScrollPane(localList), BorderLayout.CENTER);
        serverPanel.add(serverLabel, BorderLayout.NORTH);
        serverPanel.add(new JScrollPane(serverList), BorderLayout.CENTER);

        // Split panel for side-by-side display of client and server file lists
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientPanel, serverPanel);
        splitPane.setResizeWeight(0.5);

        // Add panels to main panel
        mainPanel.add(buttonPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);

        // Button actions
        uploadButton.addActionListener(e -> uploadFile());
        downloadButton.addActionListener(e -> downloadFile());
        configureButton.addActionListener(e -> openFolderConfig());
    }

    // Open folder configuration dialog
    private void openFolderConfig() {
        FolderConfigPanel configPanel = new FolderConfigPanel(folderPath -> {
            localFolder = Paths.get(folderPath);  // Update folder path
            refreshLocalFiles();  // Refresh local file list
            refreshServerFiles();  // Refresh server file list
        });
        JOptionPane.showMessageDialog(this, configPanel, "Configure Shared Folder", JOptionPane.PLAIN_MESSAGE);
    }

    // Refresh local files list by scanning the folder
    private void refreshLocalFiles() {
        localListModel.clear();  // Clear current list
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(localFolder)) {
            dirStream.forEach(path -> localListModel.addElement(path.getFileName().toString()));  // Add each file to the list
        } catch (IOException e) {
            showError("Error refreshing local files: " + e.getMessage());
        }
    }

    // Refresh server files list by querying the server
    private void refreshServerFiles() {
        serverListModel.clear();  // Clear current list
        try (Socket socket = new Socket(serverHost, 1234);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            out.write("DIR\n".getBytes("UTF-8"));  // Send command to list server files
            out.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                serverListModel.addElement(line);  // Add server file to the list
            }
        } catch (IOException e) {
            showError("Error refreshing server files: " + e.getMessage());
        }
    }

    // Upload selected file to the server
    private void uploadFile() {
        String filename = localList.getSelectedValue();
        if (filename == null) {
            showError("Please select a file to upload.");
            return;
        }

        Path filePath = localFolder.resolve(filename);
        if (!Files.exists(filePath)) {
            showError("File does not exist: " + filename);
            return;
        }

        try (Socket socket = new Socket(serverHost, 1234);
             OutputStream out = socket.getOutputStream();
             FileInputStream fileIn = new FileInputStream(filePath.toFile())) {

            out.write(("UPLOAD " + filename + "\n").getBytes("UTF-8"));
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);  // Send file content to the server
            }
            out.flush();
            refreshServerFiles();  // Refresh server file list after upload
        } catch (IOException e) {
            showError("Upload failed: " + e.getMessage());
        }
    }

    // Download selected file from the server
    private void downloadFile() {
        String filename = serverList.getSelectedValue();
        if (filename == null) {
            showError("Please select a file to download.");
            return;
        }

        Path filePath = localFolder.resolve(filename);
        try (Socket socket = new Socket(serverHost, 1234);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            out.write(("DOWNLOAD " + filename + "\n").getBytes("UTF-8"));
            out.flush();

            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);  // Save the downloaded file
            refreshLocalFiles();  // Refresh local file list after download
        } catch (IOException e) {
            showError("Download failed: " + e.getMessage());
        }
    }

    // Show error message in a dialog box
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Main method to launch the client application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (args.length != 2) {
                JOptionPane.showMessageDialog(null, "Usage: Client <server_host> <local_folder>");
                System.exit(1);
            }
            Client client = new Client(args[0], args[1]);
            client.setVisible(true);  // Show the client window
        });
    }
}
