package com.indiza.smsi.common;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.indiza.smsi.SenderActivity;
import com.indiza.smsi.data.ContactResponse;
import com.indiza.smsi.view.adapter.ContactsAdapter;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Excel Worksheet Utility Methods
 * <p>
 * Created by: Ranit Raj Ganguly on 16/04/21.
 * Performed by Envy on 16/04/2023
 */
public class ExcelUtils {
    public static final String TAG = "ExcelUtil";
    private static Cell cell;
    private static Sheet sheet;
    private static Workbook workbook;
    private static CellStyle headerCellStyle;

    private static List<ContactResponse> importedExcelData;

    /**
     * Import data from Excel Workbook
     *
     * @param context - Application Context
     * @param fileName - Name of the excel file
     * @return importedExcelData
     */
    public static List<ContactResponse> readFromExcelWorkbook(FileInputStream fileInputStream,Context context, String fileName) {
        return retrieveExcelFromStorage( fileInputStream,context, fileName);
    }


    /**
     * Export Data into Excel Workbook
     *
     * @param context  - Pass the application context
     * @param fileName - Pass the desired fileName for the output excel Workbook
     * @param dataList - Contains the actual data to be displayed in excel
     */
    public static boolean exportDataIntoWorkbook(Context context, String fileName,
                                                 List<ContactResponse> dataList) {
        boolean isWorkbookWrittenIntoStorage;

        // Check if available and not read only
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e(TAG, "Storage not available or read only");
            return false;
        }

        // Creating a New HSSF Workbook (.xls format)
        workbook = new HSSFWorkbook();

        setHeaderCellStyle();

        // Creating a New Sheet and Setting width for each column
        sheet = workbook.createSheet(Constants.EXCEL_SHEET_NAME);
        sheet.setColumnWidth(0, (15 * 400));
        sheet.setColumnWidth(1, (15 * 400));

        setHeaderRow();
        fillDataIntoExcel(dataList);
        isWorkbookWrittenIntoStorage = storeExcelInStorage(context, fileName);

        return isWorkbookWrittenIntoStorage;
    }

    /**
     * Checks if Storage is READ-ONLY
     *
     * @return boolean
     */
    private static boolean isExternalStorageReadOnly() {
        String externalStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState);
    }

    /**
     * Checks if Storage is Available
     *
     * @return boolean
     */
    private static boolean isExternalStorageAvailable() {
        String externalStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(externalStorageState);
    }

    /**
     * Setup header cell style
     */
    private static void setHeaderCellStyle() {
        headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFillForegroundColor(HSSFColor.AQUA.index);
        headerCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headerCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    }

    /**
     * Setup Header Row
     */
    private static void setHeaderRow() {
        Row headerRow = sheet.createRow(0);

        cell = headerRow.createCell(0);
        cell.setCellValue("Name");
        cell.setCellStyle(headerCellStyle);

        cell = headerRow.createCell(1);
        cell.setCellValue("Number");
        cell.setCellStyle(headerCellStyle);
    }

    /**
     * Fills Data into Excel Sheet
     * <p>
     * NOTE: Set row index as i+1 since 0th index belongs to header row
     *
     * @param dataList - List containing data to be filled into excel
     */
    private static void fillDataIntoExcel(List<ContactResponse> dataList) {
        for (int i = 0; i < dataList.size(); i++) {
            // Create a New Row for every new entry in list
            Row rowData = sheet.createRow(i + 1);

            // Create Cells for each row
            cell = rowData.createCell(0);
            cell.setCellValue(dataList.get(i).getName());

            cell = rowData.createCell(1);
            if (dataList.get(i).getPhoneNumberList() != null) {
                StringBuilder stringBuilder = new StringBuilder();

                // Nested loop: Since one user can have multiple numbers
                for (int j = 0; j < dataList.get(i).getPhoneNumberList().size(); j++) {
                    stringBuilder.append(dataList.get(i).getPhoneNumberList().get(j).getNumber())
                            .append("\n");
                }
                cell.setCellValue(String.valueOf(stringBuilder));

            } else {
                cell.setCellValue("No Phone Number");
            }
        }
    }

    /**
     * Store Excel Workbook in external storage
     *
     * @param context  - application context
     * @param fileName - name of workbook which will be stored in device
     * @return boolean - returns state whether workbook is written into storage or not
     */
    private static boolean storeExcelInStorage(Context context, String fileName) {
        boolean isSuccess;
        File file = new File(context.getExternalFilesDir(null), fileName);
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file);
            workbook.write(fileOutputStream);
            Log.e(TAG, "Writing file" + file);
            isSuccess = true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing Exception: ", e);
            isSuccess = false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save file due to Exception: ", e);
            isSuccess = false;
        } finally {
            try {
                if (null != fileOutputStream) {
                    fileOutputStream.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return isSuccess;
    }

    /**
     * Retrieve excel from External Storage
     *
     * @param context  - application context
     * @param fileName - name of workbook to be read
     * @return importedExcelData
     */
    private static List<ContactResponse> retrieveExcelFromStorage(FileInputStream fileInputStream,Context context, String fileName) {
        importedExcelData = new ArrayList<>();

       File file = new File(context.getExternalFilesDir(null), fileName);
        //File file = fichier;
        //FileInputStream fileInputStream = null;
        try {
            //fileInputStream = new FileInputStream(file);
            Log.e(TAG, "Reading from Excel " + file);

            // Create instance having reference to .xls file
            workbook = new HSSFWorkbook(fileInputStream);

            // Fetch sheet at position 'i' from the workbook
            sheet = workbook.getSheetAt(0);

            // Iterate through each row
            for (Row row : sheet) {
                int index = 0;
                List<Object> rowDataList = new ArrayList<>();
                List<ContactResponse.PhoneNumber> phoneNumberList = new ArrayList<>();

                if (row.getRowNum() > 0) {
                    // Iterate through all the columns in a row (Excluding header row)
                    Iterator<Cell> cellIterator = row.cellIterator();
                    int j = 0;
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        // Check cell type and format accordingly
                        if(cell.getCellType() == cell.CELL_TYPE_STRING) {
                            rowDataList.add(index, cell.getStringCellValue());
                        }else if(cell.getCellType() == cell.CELL_TYPE_NUMERIC && cell.getColumnIndex() ==1){
                            DataFormatter dataFormatter = new DataFormatter();
                            String formattedCellStr = dataFormatter.formatCellValue(cell);
                            rowDataList.add(index, formattedCellStr);
                            //rowDataList.add(index, cell.getNumericCellValue());
                        } else{
                            System.out.println("--Type non defini, defini le ici Excels utils-");
                            System.out.println(rowDataList);
                        }
                        j++;index++;
                    }
                    // Adding cells with phone numbers to phoneNumberList
                    for (int i = 1; i < rowDataList.size(); i++) {
                        phoneNumberList.add(new ContactResponse.PhoneNumber(rowDataList.get(i).toString()));
                    }
                    /**
                     * Index 0 of rowDataList will Always have name.
                     * So, passing it as 'name' in ContactResponse
                     *
                     * Index 1 onwards of rowDataList will have phone numbers (if >1 numbers)
                     * So, adding them to phoneNumberList
                     *
                     * Thus, importedExcelData list has appropriately mapped data
                     */
                    try{
                        importedExcelData.add(new ContactResponse(String.valueOf(index),rowDataList.get(0).toString(), phoneNumberList, true));
                        ContactsAdapter.contactsToSend = importedExcelData;
                    }catch (Exception ex){
                        Toast.makeText(context.getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }else{
                    System.out.println(row.getCell(1));
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error Reading Exception: ", e);

        } catch (Exception e) {
            Log.e(TAG, "Failed to read file due to Exception: ", e);
        } finally {
            try {
                if (null != fileInputStream) {
                    fileInputStream.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return importedExcelData;
    }

}
