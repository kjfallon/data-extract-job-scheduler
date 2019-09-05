package org.example.extractpublisher.components;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Component
public class CsvTools {

    public Boolean writeGzipCsvFileFromBeanList(List<?> itemList, String fullFilepath) {

        log.info("Received a list of " + itemList.size() + " items, writing to CSV file: " + fullFilepath);
        try {
            Writer writer = new FileWriter(fullFilepath);
            StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
            beanToCsv.write(itemList);
            writer.close();
        } catch (IOException | CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            e.printStackTrace();
            log.error("Failed to write data to csv file");
            return false;
        }

        // Now gzip the file
        byte[] buffer = new byte[1024];
        try {
            File csvFile = new File(fullFilepath);
            GZIPOutputStream gzOutputStream = new GZIPOutputStream(new FileOutputStream(fullFilepath + ".gz"));
            FileInputStream inputStream = new FileInputStream(csvFile);
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                gzOutputStream.write(buffer, 0, len);
            }
            inputStream.close();
            gzOutputStream.finish();
            gzOutputStream.close();
            csvFile.delete();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        log.info("Compressed file to " + fullFilepath + ".gz");

        return true;
    }

}
