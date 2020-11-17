package com.zions.common.services.excel

import java.security.PublicKey
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.*
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import groovy.util.logging.Slf4j
import java.io.File

import org.springframework.stereotype.Component

//import com.jayway.jsonpath.internal.function.text.Length


@Component
@Slf4j
class ExcelManagementService {
	
	
	Workbook workbook
	Sheet sheet
	Row row
	int rownum
	Map headers = [:]
	String outputFileName
	
	
	public ExcelManagementService() {
		
	}
	
	boolean openExcelFile(String fileName) {
		rownum = 0
		File excel = new File(fileName)
		if (!excel.exists())
			log.error("Not found or not a file: " + fileName)
		else {
			workbook = WorkbookFactory.create(excel)
			sheet = workbook.getSheetAt(0)
		 }
	}
	public Sheet getSheet0() {
		return workbook.getSheetAt(0)
	}
	public setHeaders(Row row) {
		int i = 0
		row.each { cell ->
			headers.put(formatCellValue(cell), ++i)
		}
		return
	}

    public readExcel() throws Exception {
        // Retrieving the number of sheets in the Workbook
        log.info("Workbook has " + workbook.getNumberOfSheets() + " Sheets : ")

        // Getting the Sheet at index zero
        Sheet sheet = workbook.getSheetAt(0);

        // Create a DataFormatter to format and get each cell's value as String
        DataFormatter dataFormatter = new DataFormatter();


        // 3. Or you can use Java 8 forEach loop with lambda
        System.out.println("\n\nIterating over Rows and Columns using Java 8 forEach with lambda\n");
        sheet.each { row -> 
            row.each { cell -> 
                printCellValue(cell);
            }
            System.out.println();
        }

        // Closing the workbook
        workbook.close();
    }
	
	/*
	 * Creates excel file in specified location
	 * Workbook is of SXSSFWorkbook type, allows us to flush from memory
	 */
	def CreateExcelFile(def dir, def filename) {
		//in case other file is open
		CloseExcelFile()
		
		outputFileName = "${dir}\\${filename}.xlsx"
		workbook = new SXSSFWorkbook(-1)
		sheet = workbook.createSheet()
		rownum = 1
		headers = [:]
		//create header
		sheet.createRow(0)
	}
	
	/*
	 * Writes out the excel file in memory
	 */
	def CloseExcelFile() {
		if (outputFileName != null) {
			log.info("Saving file to ${outputFileName}")
			try {
				File outputFile = new File(outputFileName)
				outputFile.withOutputStream { os -> workbook.write(os) }
			}
			catch (Exception e) {
				log.error("Error when saving Excel file to ${outputFileName}")
			}
			if (workbook) {
			workbook.dispose()
			}
		}
	}
//	
//	def InsertIntoCurrentRow(def value, int col) {
//		row.createCell(col).setCellValue(value)
//	}

	def InsertIntoCurrentRow(def val, String columnName) {
		//log.debug("Inserting into row ${rownum}")
		if (val.toString().length() > 32767) {
			throw new Exception("Value of ${columnName} exceeds max length of excel cell")
		}
		row.createCell(getColumn(columnName)).setCellValue(val)
	}
	
	/*
	 * Adds a column to the sheet and to the header map
	 * >if row 0 has been flushed, will it still write?
	 * >I hope this map is an ok way to do this
	 */
	def getColumn(String columnName) {
		def col = headers[columnName]
		if (col == null) {
			col = headers.size()
			headers.put(columnName, col)
			sheet.getRow(0).createCell(col).setCellValue(columnName)
		}
		return col
	}
	def getCellValue(Row row, int colNum) {
		if (colNum > 0 && colNum <= headers.size())
			if (!row.getCell(colNum-1))
				return ''
			else
				return formatCellValue(row.getCell(colNum-1))
		else {
			log.error("Invalid column number passed to getCellValue: $colNum")
			return "NA"
		}
	}
	def InsertNewRow(def values) {
		row = sheet.createRow(rownum++)
		values.each {key, val ->
			InsertIntoCurrentRow(val, key)
		}
	}
	def getRowMap(Row row) {
		def rowMap = [:]
		headers.each { header ->
			rowMap.put(header.key, getCellValue(row, header.value))
		}
		return rowMap
	}
	def getMissingColumns(List requiredColumns) {
		String missingCols
		requiredColumns.each { col ->
			if (!headers.containsKey(col)) {
				if (missingCols)
					missingCols += ",$col"
				else
					missingCols = col
			}
		}
		return missingCols
	}
	public static void printCellValue(Cell cell) {
		System.out.print(formatCellValue(cell));
		System.out.print("\t");
	}

	public def formatCellValue(Cell cell) {
		switch (cell.getCellTypeEnum()) {
			case CellType.BOOLEAN:
				return cell.getBooleanCellValue()
			case CellType.STRING:
				return cell.getRichStringCellValue().getString()
			case CellType.NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue()
				} else {
					def num = cell.getNumericCellValue()
					if (num % 1 == 0)
						return num.toInteger()
					else
						return num
				}
			case CellType.FORMULA:
				return cell.getCellFormula()
			default:
				return ''
		}

		System.out.print("\t");
	}

}
