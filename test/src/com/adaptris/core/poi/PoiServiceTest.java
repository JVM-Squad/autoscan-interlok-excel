package com.adaptris.core.poi;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.DefaultMessageFactory;
import com.adaptris.core.ServiceCase;
import com.adaptris.core.util.XmlHelper;
import com.adaptris.util.text.xml.XPath;

public class PoiServiceTest extends ServiceCase {
  public static final String KEY_SAMPLE_INPUT = "poi.sample.input";
  private DefaultMessageFactory dMessageFactory = new DefaultMessageFactory();
  
  public PoiServiceTest(String name) {
    super(name);
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    return new ExcelToXml();
  }

  @Override
  protected Object retrieveObjectForCastorRoundTrip() {
    return new ExcelToXml();
  }
  
  protected static byte[] readFile(String path) throws IOException {
    InputStream in = null;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      in = new FileInputStream(new File(path));
      IOUtils.copy(in, out);
    }
    finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(out);
    }
    return out.toByteArray();
  }
  
  public void testService() throws Exception {
    AdaptrisMessage msg = dMessageFactory.newMessage(readFile(PROPERTIES.getProperty(KEY_SAMPLE_INPUT)));
    ExcelToXml service = new ExcelToXml();
    try {
      start(service);
      service.doService(msg);
      Document d = XmlHelper.createDocument(msg);
      XPath xp = new XPath();
      assertNotNull(xp.selectSingleNode(d, "/spreadsheet/sheet[@name='Sheet1']"));
      assertTrue(xp.selectNodeList(d, "/spreadsheet/sheet[@name='Sheet1']/row/cell").getLength() > 0);
      assertNull(xp.selectSingleNode(d, "/spreadsheet/sheet[@name='Sheet1']/row/cell[@position='A1']"));
    }
    finally {
      stop(service);
    }
  }
}