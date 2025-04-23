package fileSharingSystem;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FolderConfigPanel extends JPanel {
    private JTextField folderPathField;  // Text field to display the folder path
    private JButton browseButton;        // Button to open file chooser
    private JButton saveButton;          // Button to save the folder path
    private FolderConfigListener listener;  // Listener for handling folder path selection

    // Constructor to set up the panel and add event listeners
    public FolderConfigPanel(FolderConfigListener listener) {
        this.listener = listener;

        setLayout(new BorderLayout());  // Set layout for the panel
        folderPathField = new JTextField(30);  // Text field for folder path input
        browseButton = new JButton("Browse");  // Browse button to choose folder
        saveButton = new JButton("Save");  // Save button to save the folder path

        // Panel to hold the components (label, text field, and browse button)
        JPanel controlPanel = new JPanel();
        controlPanel.add(new JLabel("Shared Folder Path:"));
        controlPanel.add(folderPathField);
        controlPanel.add(browseButton);

        // Add the control panel and save button to the main panel
        add(controlPanel, BorderLayout.CENTER);
        add(saveButton, BorderLayout.SOUTH);

        // Event listener for the browse button to open folder chooser
        browseButton.addActionListener(e -> chooseFolder());
        // Event listener for the save button to save the folder path
        saveButton.addActionListener(e -> saveSettings());
    }

    // Method to open file chooser dialog for selecting a folder
    private void chooseFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);  // Restrict to directories
        int returnVal = fileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            // Set the selected folder path in the text field
            folderPathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Method to save the folder path and notify the listener
    private void saveSettings() {
        String folderPath = folderPathField.getText();
        listener.onFolderSelected(folderPath);  // Notify listener with the selected folder path
    }

    // Interface to notify when a folder is selected
    public interface FolderConfigListener {
        void onFolderSelected(String folderPath);
    }
}
