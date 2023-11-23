import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

public class ServiceFileCalculateFormat extends Task<Boolean> {

    private Set<File> listFiles;

    private HashMap<String, Integer> countFormat = new HashMap<>();
    private ArrayList<String> unknownFormatList = new ArrayList<>();

    private long countListInFile = 0;
    private int countFile = 0;

    private final int constDPI = 72;

    //Конструктор
    public ServiceFileCalculateFormat(Set<File> listFiles) {
        this.listFiles = listFiles;
    }

    @Override
    protected Boolean call() {
        //Проверяем файлы
        if (listFiles == null){
            return false;
        }

        for (File folder : listFiles) {
            if (folder.isDirectory()) {
                checkFolder(folder);
            } else {
                if (folder.getName().toLowerCase().endsWith(".pdf")) {
                    checkPDF_file(folder);
                } else {
                    if (folder.getName().toLowerCase().endsWith(".tif") || folder.getName().toLowerCase().endsWith(".jpg")) {
                        checkTiffJPG_file(folder);
                    }
                }
            }
        }
        updateProgress(1, 1);   //updateProgress(текущее значение, максимальное значение);
        return true;
    }

    private void checkFolder(File folder) {
        FileFilter fileFilter = file -> {
            if (file.getName().toLowerCase().endsWith(".pdf") || file.getName().toLowerCase().endsWith(".tif") || file.getName().toLowerCase().endsWith(".jpg") || file.isDirectory()) {
                return true;
            }
            return false;
        };

        for (File file : Objects.requireNonNull(folder.listFiles(fileFilter))) {
            if (!file.isDirectory()) {
                if (file.getName().toLowerCase().endsWith(".pdf")) {
                    checkPDF_file(file);
                } else {
                    checkTiffJPG_file(file);
                }
            } else {
                checkFolder(file);              //Применяем рекурсию
            }
        }
    }

    private void checkTiffJPG_file(File file) {

        countFile++;

        try {
            ImageInputStream isb = ImageIO.createImageInputStream(file);

            ImageInfo imageInfo = Sanselan.getImageInfo(file);

            int widthDpi = imageInfo.getPhysicalWidthDpi();
            int heightDpi = imageInfo.getPhysicalHeightDpi();

            Iterator<ImageReader> iterator = ImageIO.getImageReaders(isb);
            if (iterator == null || !iterator.hasNext()) {
                throw new IOException("Image file format not supported by ImageIO: ");
            }

            ImageReader reader = (ImageReader) iterator.next();
            reader.setInput(isb);

            int nbPages = reader.getNumImages(true);

            //Забиваем на многостраничность файла!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            //(Потому что размер последующих листов определяется неадекватно из-за качества самих файлов )

            BufferedImage bufferedImage = reader.read(0);   //Читаем только первый лист
            int height = pt_To_mm(bufferedImage.getHeight(), heightDpi);
            int width = pt_To_mm(bufferedImage.getWidth(), widthDpi);

            String formatPage;
            if (height < width) {
                //Альбомная ориентация"
                //Определяем формат
                formatPage = getFormat(height, width);
            } else {
                //Кижная ориентация
                //Определяем формат
                formatPage = getFormat(width, height);
            }

            //Костыль чтобы обойти многостраничность файла tif и при этом не переписывать методы!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            for (int i = 0; i < nbPages; i++) {
                if (!formatPage.equals("Unknown")) {
                    calculateCountFormat(formatPage);
                } else {
                    unknownFormatList.add(file.getName() + " лист " + i);
                }
            }

            //Прибавляем количество проверенных листов к общему количеству листов
            countListInFile += (nbPages);

            //Закрываем поток
            isb.close();

        } catch (IOException | ImageReadException e) {
            e.printStackTrace();
        }
    }

    /***/
    private void checkPDF_file(File file) {
        countFile++;
        try {
            PDDocument doc = PDDocument.load(file);
            assert doc != null;
            PDPageTree pdPageTree = doc.getPages();

            int x = 1;                                      //Номер листа в документе
            for (PDPage pd : pdPageTree) {

                int height = pt_To_mm(pd.getTrimBox().getHeight(), constDPI);
                int width = pt_To_mm(pd.getTrimBox().getWidth(), constDPI);

                if (height < width) {
                    //Альбомная ориентация"
                    //Определяем формат
                    String formatPage = getFormat(height, width);

                    if (!formatPage.equals("Unknown")) {
                        calculateCountFormat(formatPage);
                    } else {
                        unknownFormatList.add(file.getName() + " лист " + x);
                    }

                } else {
                    //Кижная ориентация
                    //Определяем формат
                    String formatPage = getFormat(width, height);

                    if (!formatPage.equals("Unknown")) {
                        calculateCountFormat(formatPage);
                    } else {
                        unknownFormatList.add(file.getName() + " лист " + x);
                    }
                }
                x++;
            }

            //Прибавляем количество проверенных листов к общему количеству листов
            countListInFile += doc.getNumberOfPages();
            doc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***/
    private void calculateCountFormat(String format) {
        if (countFormat.containsKey(format)) {
            int x = countFormat.get(format) + 1;
            countFormat.put(format, x);
        } else {
            countFormat.put(format, 1);
        }
    }


    //===================== Геттеры =======================================
    private String getFormat(int width, int height) {

        //Формат А4
        if (width >= (210 - 210 * 0.1) && (width <= (210 + 210 * 0.1))) {
            if (height >= (297 - 297 * 0.1) && (height <= (297 + 297 * 0.1))) {
                return "A4";
            }
        }

        //Формат А3
        if (width >= (297 - 297 * 0.1) && (width <= (297 + 297 * 0.1))) {
            if (height >= (420 - 420 * 0.1) && (height <= (420 + 420 * 0.1))) {
                return "A3";
            }

            //Формат А4x3
            if (height >= (630 - 630 * 0.1) && (height <= (630 + 630 * 0.1))) {
                return "A4x3";
            }

            //Формат А4x4
            if (height >= (841 - 841 * 0.1) && (height <= (841 + 841 * 0.1))) {
                return "A4x4";
            }

            //Формат А4x5
            if (height >= (1051 - 1051 * 0.1) && (height <= (1051 + 1051 * 0.1))) {
                return "A4x5";
            }

            //Формат А4x6
            if (height >= (1261 - 1261 * 0.1) && (height <= (1261 + 1261 * 0.1))) {
                return "A4x6";
            }

            //Формат А4x7
            if (height >= (1471 - 1471 * 0.1) && (height <= (1471 + 1471 * 0.1))) {
                return "A4x7";
            }

            //Формат А4x8
            if (height >= (1682 - 1682 * 0.1) && (height <= (1682 + 1682 * 0.1))) {
                return "A4x8";
            }

            //Формат А4x9
            if (height >= (1892 - 1892 * 0.1) && (height <= (1892 + 1892 * 0.1))) {
                return "A4x9";
            }
        }

        //Формат А2
        if (width >= (420 - 420 * 0.1) && (width <= (420 + 420 * 0.1))) {
            if (height >= (594 - 594 * 0.1) && (height <= (594 + 594 * 0.1))) {
                return "A2";
            }

            //Формат А3x3
            if (height >= (891 - 891 * 0.1) && (height <= (891 + 891 * 0.1))) {
                return "A3x3";
            }

            //Формат А3x4
            if (height >= (1189 - 1189 * 0.1) && (height <= (1189 + 1189 * 0.1))) {
                return "A3x4";
            }

            //Формат А3x5
            if (height >= (1486 - 1486 * 0.1) && (height <= (1486 + 1486 * 0.1))) {
                return "A3x5";
            }

            //Формат А3x6
            if (height >= (1783 - 1783 * 0.1) && (height <= (1783 + 1783 * 0.1))) {
                return "A3x6";
            }

            //Формат А3x7
            if (height >= (2080 - 2080 * 0.1) && (height <= (2080 + 2080 * 0.1))) {
                return "A3x7";
            }
        }

        //Формат А1
        if (width >= (594 - 594 * 0.1) && (width <= (594 + 594 * 0.1))) {
            if (height >= (841 - 841 * 0.1) && (height <= (841 + 841 * 0.1))) {
                return "A1";
            }

            //Формат А2x3
            if (height >= (1261 - 1261 * 0.1) && (height <= (1261 + 1261 * 0.1))) {
                return "A2x3";
            }

            //Формат А2x4
            if (height >= (1682 - 1682 * 0.1) && (height <= (1682 + 1682 * 0.1))) {
                return "A2x4";
            }

            //Формат А2x5
            if (height >= (2102 - 2102 * 0.1) && (height <= (2102 + 2102 * 0.1))) {
                return "A2x5";
            }
        }

        //Формат А0
        if (width >= (841 - 841 * 0.1) && (width <= (841 + 841 * 0.1))) {
            if (height >= (1189 - 1189 * 0.1) && (height <= (1189 + 1189 * 0.1))) {
                return "A0";
            }

            //Формат А1x3
            if (height >= (1783 - 1783 * 0.1) && (height <= (1783 + 1783 * 0.1))) {
                return "A1x3";
            }

            //Формат А1x4
            if (height >= (2378 - 2378 * 0.1) && (height <= (2378 + 2378 * 0.1))) {
                return "A1x4";
            }
        }

        //Формат A0x2
        if (width >= (1189 - 1189 * 0.1) && (width <= (1189 + 1189 * 0.1))) {
            if (height >= (1682 - 1682 * 0.1) && (height <= (1682 + 1682 * 0.1))) {
                return "A0x2";
            }

            //Формат A0x3
            if (height >= (2523 - 2523 * 0.1) && (height <= (2523 + 2523 * 0.1))) {
                return "A0x3";
            }
        }


        //Формат А5
        if (width >= (148 - 148 * 0.1) && (width <= (148 + 148 * 0.1))) {
            if (height >= (210 - 210 * 0.1) && (height <= (210 + 210 * 0.1))) {
                return "A5";
            }
        }

        //Формат А6
        if (width >= (105 - 105 * 0.1) && (width <= (105 + 105 * 0.1))) {
            if (height >= (148 - 148 * 0.1) && (height <= (148 + 148 * 0.1))) {
                return "A6";
            }
        }
        return "Unknown";
    }

    private int pt_To_mm(float pt, float dpi) {
        float result = pt * 25.4f / dpi;
        return (int) result;
    }

    public HashMap<String, Integer> getCountFormat() {
        return countFormat;
    }

    public ArrayList<String> getUnknownFormatList() {
        return unknownFormatList;
    }

    public long getCountListInFile() {
        return countListInFile;
    }

    public int getCountFile() {
        return countFile;
    }


}

