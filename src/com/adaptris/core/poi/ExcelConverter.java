package com.adaptris.core.poi;

import static com.adaptris.core.poi.ExcelHelper.XML_ATTR_POSITION;
import static com.adaptris.core.poi.ExcelHelper.XML_ATTR_ROW_NUMBER;
import static com.adaptris.core.poi.ExcelHelper.XML_ATTR_SHEET_NAME;
import static com.adaptris.core.poi.ExcelHelper.XML_ATTR_TYPE;
import static com.adaptris.core.poi.ExcelHelper.XML_ELEMENT_ROW;
import static com.adaptris.core.poi.ExcelHelper.XML_ELEMENT_SPREADSHEET;
import static com.adaptris.core.poi.ExcelHelper.XML_ELEMENT_WORKSHEET;
import static com.adaptris.core.poi.ExcelHelper.createCellName;
import static com.adaptris.core.poi.ExcelHelper.getCellCount;
import static com.adaptris.core.poi.ExcelHelper.getRowCount;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.adaptris.core.poi.ExcelHelper.CellHandler;
import com.adaptris.core.poi.XmlStyle.ElementNaming;
import com.adaptris.util.XmlUtils;

class ExcelConverter {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  public final Document convertToXml(HSSFWorkbook workbook, XmlStyle styleGuide) throws Exception {
    XmlUtils xmlUtils = new XmlUtils();
    Document document = (Document) XmlUtils.createDocument();

    log.trace("workbook has " + workbook.getNumberOfSheets() + " sheets");
    Element rootElement = document.createElement(XML_ELEMENT_SPREADSHEET);
    document.appendChild(rootElement);
    for (int sheetCounter = 0; sheetCounter < workbook.getNumberOfSheets(); sheetCounter++) {
      HSSFSheet sheet = workbook.getSheetAt(sheetCounter);
      processWorksheet(sheet, rootElement, styleGuide);
    }
    return document;
  }

  private void processWorksheet(HSSFSheet sheet, Element parent, XmlStyle styleGuide) throws Exception {
    Document document = parent.getOwnerDocument();
    Element sheetElement = document.createElement(XML_ELEMENT_WORKSHEET);
    parent.appendChild(sheetElement);
    sheetElement.setAttribute(XML_ATTR_SHEET_NAME, sheet.getSheetName());
    int nRows = getRowCount(sheet);
    int nCells = getCellCount(sheet);
    log.trace("Sheet " + sheet.getSheetName() + " has " + nRows + " rows with " + nCells + " cells");
    int rowCounter = sheet.getFirstRowNum();
    String[] columnNames = createColumnNames(sheet, styleGuide);
    if (styleGuide.resolveNamingStrategy() == ElementNaming.HEADER_ROW) {
      rowCounter = styleGuide.headerRow();
    }
    // Now loop through and create each row.
    for (; rowCounter < nRows; rowCounter++) {
      HSSFRow row = sheet.getRow(rowCounter);
      if (row == null) {
        throw new Exception("Unable to get row " + (rowCounter + 1));
      }
      processRow(row, sheetElement, styleGuide, columnNames);
    }
  }

  private void processRow(HSSFRow row, Element parent, XmlStyle styleGuide, String[] columnNames) throws Exception {
    Document document = parent.getOwnerDocument();
    Element rowElement = document.createElement(XML_ELEMENT_ROW);
    parent.appendChild(rowElement);
    int rowCounter = row.getRowNum() + 1;
    log.trace("Processing row " + rowCounter);
    if (styleGuide.getEmitRowNumberAttr()) {
      rowElement.setAttribute(XML_ATTR_ROW_NUMBER, String.valueOf(rowCounter));
    }
    for (int i = 0; i < columnNames.length; i++) {
      log.trace("Creating element " + columnNames[i]);
      Element cellElement = document.createElement(columnNames[i]);
      rowElement.appendChild(cellElement);
      HSSFCell cell = row.getCell(i);
      CellHandler handler = ExcelHelper.getHandler(cell);
      String value = handler.getValue(cell, styleGuide);
      String type = handler.getType();
      log.trace("Cell(" + rowCounter + "," + (i + 1) + ") is a [" + type + "] and the computed value is [" + value + "]");
      // if showDataTypes is true, add the attribute to the xml node
      if (styleGuide.getEmitDataTypeAttr()) {
        cellElement.setAttribute(XML_ATTR_TYPE, type);
      }
      // if showCellPosition is true, add the positional element to the XML (e.g. BB0).
      if (styleGuide.getEmitCellPositionAttr()) {
        cellElement.setAttribute(XML_ATTR_POSITION, createCellName(i) + rowCounter);
      }
      // set the value of the xml node
      cellElement.setTextContent(value);
    }
  }

  private String[] createColumnNames(HSSFSheet sheet, XmlStyle lf) {
    return lf.resolveNamingStrategy().createColumnNames(sheet, lf);
  }


}
