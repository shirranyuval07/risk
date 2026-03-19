package View;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import java.util.Optional;

public class LoginDialog {


    public static Optional<Pair<String, String>> show(String title) {

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Please enter your credentials:");

        // 2. הוספת כפתורים לחלון (למשל כפתור "אישור" וכפתור "ביטול")
        // המשימה שלך: ליצור ButtonType עבור אישור (למשל "Submit") ולהוסיף אותו לדיאלוג.
        ButtonType submitButton = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButton, cancelButton);


        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        GridPane grid = new GridPane();
        grid.add(new Label("Username:"), 0, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(usernameField, 1, 0);
        grid.add(passwordField, 1, 1);
        dialog.getDialogPane().setContent(grid);




        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButton) {
                return new Pair<>(usernameField.getText(), passwordField.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }
}