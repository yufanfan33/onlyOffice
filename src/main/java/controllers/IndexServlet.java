/*
 *
 * (c) Copyright Ascensio System Limited 2010-2018
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/


package controllers;

import helpers.DocumentManager;
import helpers.ServiceConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Scanner;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import entities.FileType;
import helpers.FileUtility;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.primeframework.jwt.domain.JWT;

@WebServlet(name = "IndexServlet", urlPatterns = {"/IndexServlet"})
@MultipartConfig
public class IndexServlet extends HttpServlet {
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("type");
        System.out.println("---------------------index----------------------");
        System.out.println("action是"+action);
        if (action == null)
        {
            request.getRequestDispatcher("index.jsp").forward(request, response);
            return;
        }

        DocumentManager.Init(request, response);
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();

        switch (action.toLowerCase())
        {
            case "upload":
                Upload(request, response, writer);
                break;
            case "convert":
                Convert(request, response, writer);
                break;
            case "track":
                Track(request, response, writer);
                break;
        }
    }


    private static void Upload(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        response.setContentType("text/plain");
        
        try
        {
        	System.out.println("--------------进入upload---------------");
            Part httpPostedFile = request.getPart("file");

            String fileName = "";
            for (String content : httpPostedFile.getHeader("content-disposition").split(";"))
            {
                if (content.trim().startsWith("filename"))
                {
                    fileName = content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
                }
            }

            long curSize = httpPostedFile.getSize();
            if (DocumentManager.GetMaxFileSize() < curSize || curSize <= 0)
            {
                writer.write("{ \"error\": \"File size is incorrect\"}");
                return;
            }

            String curExt = FileUtility.GetFileExtension(fileName);
            if (!DocumentManager.GetFileExts().contains(curExt))
            {
                writer.write("{ \"error\": \"File type is not supported\"}");
                return;
            }

            InputStream fileStream = httpPostedFile.getInputStream();

            fileName = DocumentManager.GetCorrectName(fileName);
            System.out.println("fileName是："+fileName);
            String fileStoragePath = DocumentManager.StoragePath(fileName, null);
            System.out.println("fileStoragePath是："+fileStoragePath);
            File file = new File(fileStoragePath);

            try (FileOutputStream out = new FileOutputStream(file))
            {
                int read;
                final byte[] bytes = new byte[1024];
                while ((read = fileStream.read(bytes)) != -1)
                {
                    out.write(bytes, 0, read);
                }

                out.flush();
            }

            writer.write("{ \"filename\": \"" + fileName + "\"}");

        }
        catch (IOException | ServletException e)
        {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void Convert(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
        response.setContentType("text/plain");

        try
        {
        	System.out.println("---------------------进入convert---------------------");
            String fileName = request.getParameter("filename");


//            String fileUri = DocumentManager.GetFileUri(fileName);
            //获取minio服务器URL
            String fileUri = "http://106.13.57.201:9000/test/202011/sample.docx?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20201121%2F%2Fs3%2Faws4_request&X-Amz-Date=20201121T120302Z&X-Amz-Expires=432000&X-Amz-SignedHeaders=host&X-Amz-Signature=5d5e2a91f86ba205190f8e8e2a8888fe9f35fad8ae5875a766aa1b107d190022";
            System.out.println("fileName是："+fileName);
            System.out.println("fileUri是："+fileUri);
            String fileExt = FileUtility.GetFileExtension(fileName);
            FileType fileType = FileUtility.GetFileType(fileName);
            String internalFileExt = DocumentManager.GetInternalExtension(fileType);

            if (DocumentManager.GetConvertExts().contains(fileExt))
            {
                String key = ServiceConverter.GenerateRevisionId(fileUri);

                String newFileUri = ServiceConverter.GetConvertedUri(fileUri, fileExt, internalFileExt, key, true);
                System.out.println("newFileUri是："+newFileUri);
                if (newFileUri.isEmpty())
                {
                    writer.write("{ \"step\" : \"0\", \"filename\" : \"" + fileName + "\"}");
                    return;
                }

                String correctName = DocumentManager.GetCorrectName(FileUtility.GetFileNameWithoutExtension(fileName) + internalFileExt);

                URL url = new URL(newFileUri);
                System.out.println("url是："+url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();

                if (stream == null)
                {
                    throw new Exception("Stream is null");
                }

                File convertedFile = new File(DocumentManager.StoragePath(correctName, null));
                try (FileOutputStream out = new FileOutputStream(convertedFile))
                {
                    int read;
                    final byte[] bytes = new byte[1024];
                    while ((read = stream.read(bytes)) != -1)
                    {
                        out.write(bytes, 0, read);
                    }

                    out.flush();
                }

                connection.disconnect();

                //remove source file ?
                //File sourceFile = new File(DocumentManager.StoragePath(fileName, null));
                //sourceFile.delete();

                fileName = correctName;
            }

            writer.write("{ \"filename\" : \"" + fileName + "\"}");

        }
        catch (Exception ex)
        {
            writer.write("{ \"error\": \"" + ex.getMessage() + "\"}");
        }
    }

    private static void Track(HttpServletRequest request, HttpServletResponse response, PrintWriter writer)
    {
    	System.out.println("----------------------进入track!!!---------------------");
    	
        String userAddress = request.getParameter("userAddress");
        String fileName = request.getParameter("fileName");
        //minio
//        String storagePath = DocumentManager.StoragePath(fileName, userAddress);

        System.out.println("userAddress是："+userAddress);
        System.out.println("fileName是："+fileName);
//        System.out.println("storagePath是："+storagePath);
        String body = "";

        try
        {
            Scanner scanner = new Scanner(request.getInputStream());
            scanner.useDelimiter("\\A");
            body = scanner.hasNext() ? scanner.next() : "";
            scanner.close();
        }
        catch (Exception ex)
        {
            writer.write("get request.getInputStream error:" + ex.getMessage());
            return;
        }

        if (body.isEmpty())
        {
            writer.write("empty request.getInputStream");
            return;
        }

        JSONParser parser = new JSONParser();
        JSONObject jsonObj;

        try
        {
        	System.out.println("body是:"+body);
            Object obj = parser.parse(body);
            jsonObj = (JSONObject) obj;
        }
        catch (Exception ex)
        {
            writer.write("JSONParser.parse error:" + ex.getMessage());
            return;
        }

        // int status;
        int status=99;//随便初始化的
        String downloadUri;

        System.out.println("即将判断DocumentManager.TokenEnabled()");
        if (DocumentManager.TokenEnabled())
        {
        	System.out.println("结果为真");
            String token = (String) jsonObj.get("token");

            JWT jwt = DocumentManager.ReadToken(token);
            if (jwt == null)
            {
                writer.write("JWT.parse error");
                return;
            }

            status = jwt.getInteger("status");
            downloadUri = jwt.getString("url");
            System.out.println("1:status是："+status);
            System.out.println("1:downloadUri是："+downloadUri);
        } else {
        	System.out.println("结果为假");
        	System.out.println("到这里了吗0");
        	System.out.println(jsonObj);
        	System.out.println(jsonObj.get("status")+"!!!");
            status = Integer.valueOf(jsonObj.get("status").toString());
            System.out.println("到这里了吗1");
            downloadUri = (String) jsonObj.get("url");
            System.out.println("到这里了吗2");
            System.out.println("2:status是："+status);
            System.out.println("2:downloadUri是："+downloadUri);
            System.out.println("到这里了吗3");
        }

        int saved = 0;
        if (status == 2 || status == 3)//MustSave, Corrupted
        {
            try
            {
                URL url = new URL(downloadUri);
                System.out.println("downloadUri是："+downloadUri);
                System.out.println("url是："+url);
                
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                InputStream stream = connection.getInputStream();

                if (stream == null)
                {
                    throw new Exception("Stream is null");
                }
                //实时更新文件至minio服务器
                System.out.println("执行的是实时更新文件至minio服务器～～～～～～～");

//                File savedFile = new File(storagePath);
//                try (FileOutputStream out = new FileOutputStream(savedFile))
//                {
//                    int read;
//                    final byte[] bytes = new byte[1024];
//                    while ((read = stream.read(bytes)) != -1)
//                    {
//                        out.write(bytes, 0, read);
//                    }
//
//                    out.flush();
//                }
//
//                connection.disconnect();

            }
            catch (Exception ex)
            {
                saved = 1;
            }
        }

        writer.write("{\"error\":" + saved + "}");
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	System.out.println("----------进入doGet----------");
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
    	System.out.println("----------进入doPost----------");
        processRequest(request, response);
    }

    @Override
    public String getServletInfo()
    {
        return "Handler";
    }
}
