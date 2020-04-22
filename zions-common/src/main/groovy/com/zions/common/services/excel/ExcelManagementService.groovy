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
	Map headers
	String outputFileName
	
	
	public ExcelManagementService() {
		
	}
	
	def OpenExcelFile(String fileName) {
		rownum = 0
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
	
	def InsertNewRow(def values) {
		row = sheet.createRow(rownum++)
		values.each {key, val ->
			InsertIntoCurrentRow(val, key)
		}
	}
	
	
	
	
}
