import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class Arhivarius extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("arhivarius_main_screen.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("Архивариус 29 КТЦ");
        InputStream iconStream = getClass().getResourceAsStream("29ktc_96x96.jpg");
        Image image = new Image(iconStream);
        stage.getIcons().add(image);

        stage.show();
    }

    @Override
    public void init() throws Exception {
        super.init();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}
