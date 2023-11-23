import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class Controller implements Initializable {

    private final HashMap<String, Integer> count_A4_in_Format = new HashMap<>();
    private File folder;
    private Set<File> listFiles = new HashSet<>();
    private ServiceFileCalculateFormat serviceFileCalculateFormat;

    @FXML
    private Button btnCalculateFormat;

    @FXML
    private Button btnChooseFile;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button btnChooseFolder;

    @FXML
    private Label nameFileLable;

    @FXML
    private TextArea textArea;

    @FXML
    private Button btnReset;

    @FXML
    void onClickReset(MouseEvent event) {
        reset();
    }

    //Метод срабатывает когда перетаскиваемый объект находится сверху
    @FXML//Разобраться как работает!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    void onDragOver(DragEvent event) {
        if (event.getGestureSource() != textArea
                && event.getDragboard().hasFiles()) {
            /* allow for both copying and moving, whatever user chooses */
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    @FXML
    void onDragDroped(DragEvent event) {
        System.out.println("onDragDroped");
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            listFiles.addAll(db.getFiles());
            textArea.setBackground(null);
            nameFileLable.setText("Выбрано файлов и папок: " + listFiles.size());
            success = true;
        }
        /* let the source know whether the string was successfully
         * transferred and used */
        event.setDropCompleted(success);

        event.consume();
    }

    //Метод
    @FXML
    void onDragEntered(DragEvent event) {
        System.out.println("onDragEntered");
        if (event.getGestureSource() != textArea &&
                event.getDragboard().hasFiles()) {
            InputStream backgroundStream = getClass().getResourceAsStream("background.jpg");
            textArea.setBackground(new Background(
                    new BackgroundImage(
                            new Image(backgroundStream),
                            BackgroundRepeat.REPEAT,
                            BackgroundRepeat.REPEAT,
                            BackgroundPosition.CENTER,
                            BackgroundSize.DEFAULT)));
            try {
                backgroundStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                showErrorDialog("Произошел сбой! Срочно обратитесь к разработчику!");
            }
        }
        event.consume();
    }

    @FXML
    void onDragExited(DragEvent event) {
        textArea.setBackground(null);
    }

    @FXML
    void onClickChooseFile(MouseEvent event) {
        //Обнуляем все переменные
        reset();

        FileChooser fileChooser = new FileChooser();
        configuringFileChooser(fileChooser);
        folder = fileChooser.showOpenDialog(btnChooseFolder.getScene().getWindow());
        if (folder != null) {
            nameFileLable.setText("Выбран файл: " + folder.getName());
            listFiles.add(folder);
        }
    }

    @FXML
    void onClickChooseFolder(MouseEvent event) {
        //Обнуляем все переменные
        reset();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        configuringDirectoryChooser(directoryChooser);

        folder = directoryChooser.showDialog(btnChooseFolder.getScene().getWindow());
        if (folder != null) {
            nameFileLable.setText("Выбрана папка: " + folder.getName());

            listFiles.addAll(Arrays.asList(Objects.requireNonNull(folder.listFiles())));
        }
    }

    @FXML
    void onClickCalculateFormat(ActionEvent event) {

        //Отключаем кнопки, чтобы пользователь не мешал работе программы
        setDisableBtn(true);

        serviceFileCalculateFormat = new ServiceFileCalculateFormat(listFiles);

        //Привязываем progressBar к serviceFileCalculateFormat
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(serviceFileCalculateFormat.progressProperty());


        //Обработчики события завершения работы вспомогательного потока
        serviceFileCalculateFormat.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                workerStateEvent.getSource().getException().printStackTrace();
            }
        });

        serviceFileCalculateFormat.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {

                textArea.setText(printTextForFile());

                //Включаем кнопки после завершения работы программы
                setDisableBtn(false);
            }
        });

        new Thread(serviceFileCalculateFormat).start();
    }


    private String printTextForFile() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Всего листов в формате А4: " + calculateCount_A4(serviceFileCalculateFormat.getCountFormat()) + "\n\n");
        stringBuilder.append("Для справки:\n");
        stringBuilder.append("Всего проверено документов: " + serviceFileCalculateFormat.getCountFile() + "\n");
        stringBuilder.append("Всего листов в документах: " + serviceFileCalculateFormat.getCountListInFile() + "\n");
        stringBuilder.append("Из них распознано форматов:" + "\n");
        serviceFileCalculateFormat.getCountFormat().forEach((key, val) -> {
            if (val != 0) {
                stringBuilder.append(key + " : " + val + "\n");
            }
        });

        if (serviceFileCalculateFormat.getUnknownFormatList().size() != 0) {
            stringBuilder.append("\n");
            stringBuilder.append("Всего листов с неизвестным форматом: " + serviceFileCalculateFormat.getUnknownFormatList().size() + "\n");
            stringBuilder.append("Необходимо проверить в ручную следующие листы:\n");
            serviceFileCalculateFormat.getUnknownFormatList().forEach(str -> stringBuilder.append(str + "\n"));
        }
        return stringBuilder.toString();
    }

    private void configuringDirectoryChooser(DirectoryChooser directoryChooser) {
        directoryChooser.setTitle("Выберете папку с файлами");
        if (folder != null) {
            directoryChooser.setInitialDirectory(folder.getParentFile());
        } else {
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }
    }

    private void configuringFileChooser(FileChooser fileChooser) {
        fileChooser.setTitle("Выберете файл");

        if (folder != null) {
            fileChooser.setInitialDirectory(folder.getParentFile());
        } else {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        }

    }

    private void reset() {
        serviceFileCalculateFormat = null;
        listFiles.clear();
        textArea.setText("Файлы можно добавить перетаскиванием");
        nameFileLable.setText("Выбрана папка: ");
    }


    private int calculateCount_A4(HashMap<String, Integer> countFormat) {
        int count_A4 = 0;
        for (String str : countFormat.keySet()) {
            if (!str.equals("A5")) {
                count_A4 += countFormat.get(str) * count_A4_in_Format.get(str);
            } else {
                float x = ((countFormat.get(str) * count_A4_in_Format.get(str)) * 0.5f);
                count_A4 += Math.round(x);
            }
        }
        return count_A4;
    }

    private void setDisableBtn(Boolean bool) {
        btnCalculateFormat.setDisable(bool);
        btnChooseFile.setDisable(bool);
        btnChooseFolder.setDisable(bool);
    }

    private void showErrorDialog(String textError){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setContentText(textError);
            alert.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        count_A4_in_Format.put("A0", 16);
        count_A4_in_Format.put("A1", 8);
        count_A4_in_Format.put("A2", 4);
        count_A4_in_Format.put("A3", 2);
        count_A4_in_Format.put("A4", 1);
        count_A4_in_Format.put("A5", 1);     //0,5   -   обработать программно

        count_A4_in_Format.put("A0x2", 32);
        count_A4_in_Format.put("A0x3", 48);

        count_A4_in_Format.put("A1x3", 24);
        count_A4_in_Format.put("A1x4", 32);

        count_A4_in_Format.put("A2x3", 12);
        count_A4_in_Format.put("A2x4", 16);
        count_A4_in_Format.put("A2x5", 20);

        count_A4_in_Format.put("A3x3", 6);
        count_A4_in_Format.put("A3x4", 8);
        count_A4_in_Format.put("A3x5", 10);
        count_A4_in_Format.put("A3x6", 12);
        count_A4_in_Format.put("A3x7", 14);

        count_A4_in_Format.put("A4x3", 3);
        count_A4_in_Format.put("A4x4", 4);
        count_A4_in_Format.put("A4x5", 5);
        count_A4_in_Format.put("A4x6", 6);
        count_A4_in_Format.put("A4x7", 7);
        count_A4_in_Format.put("A4x8", 8);
        count_A4_in_Format.put("A4x9", 9);
    }
}