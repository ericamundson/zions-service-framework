package com.zions.common.services.excel

import java.security.PublicKey
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.*
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import groovy.util.logging.Slf4j
import java.io.File

import org.springframework.stereotype.Component


@Component
@Slf4j
class ExcelManagementService {
	
	
	Workbook workbook
	Sheet sheet
	Row row
	int rowNum
	Map headers
	String outputFileName
	
	
	public ExcelManagementService() {
		
	}
	
	def OpenExcelFile(String fileName) {
		rowNum = 0
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
		rowNum = 0
		headers = [:]
	}
	
	/*
	 * Writes out the excel file in memory
	 */
	def CloseExcelFile() {
		if (outputFileName != null)
			log.debug("Saving file to ${outputFileName}")
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
//	
//	def InsertIntoCurrentRow(def value, int col) {
//		row.createCell(col).setCellValue(value)
//	}

	def InsertIntoCurrentRow(def val, String colname) {
		def col = headers[colname]
		if (!col) {
			col = AddColumn(colname)
		}
		row.createCell(col).setCellValue(val)
	}
	
	/*
	 * Adds a column to the sheet and to the header map
	 * >if row 0 has been flushed, will it still write?
	 * >I hope this map is an ok way to do this
	 */
	def AddColumn(String columnName) {
		int col = headers.size() + 1
		headers.put(columnName, col)
		

		//before trying the wild ride below, how about:
		sheet.getRow(0).createCell(col).setCellValue(columnName)
		return col
//		//it would seem directly adding a column that doesn't exist is a bit of a dance:
//		Iterator<Row> iterator = sheet.iterator();
//			while (iterator.hasNext()) {
//				Row currentRow = iterator.next();
//				Cell cell = currentRow.createCell(currentRow.getLastCellNum(), CellType.STRING);
//				if(currentRow.getRowNum() == 0)
//					cell.setCellValue("NEW-COLUMN");
//			}
	}
	
	def InsertNewRow(def values) {
		row = sheet.createRow(rowNum++)
		values.each {key, val ->
			InsertIntoCurrentRow(val, key)
		}
	}
	
	
	
	
}
