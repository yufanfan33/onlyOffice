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

package helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.plaf.synth.SynthSpinnerUI;

import entities.FileType;

import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;

public class DocumentManager
{
    private static HttpServletRequest request;

    public static void Init(HttpServletRequest req, HttpServletResponse resp)
    {
    	System.out.println("-----------进入DocumentManeger中的Init----------");
        request = req;
    }

    public static long GetMaxFileSize()
    {
    	System.out.println("-----------进入DocumentManeger中的GetMaxFileSize----------");
        long size;

        try
        {
            size = Long.parseLong(ConfigManager.GetProperty("filesize-max"));
        }
        catch (Exception ex)
        {
            size = 0;
        }

        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public static List<String> GetFileExts()
    {
    	System.out.println("-----------进入DocumentManeger中的GetFileExts----------");
        List<String> res = new ArrayList<>();

        res.addAll(GetViewedExts());
        res.addAll(GetEditedExts());
        res.addAll(GetConvertExts());
        
        return res;
    }

    public static List<String> GetViewedExts()
    {
    	System.out.println("-----------进入DocumentManeger中的GetViewedExts----------");
        String exts = ConfigManager.GetProperty("files.docservice.viewed-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static List<String> GetEditedExts()
    {
    	System.out.println("-----------进入DocumentManeger中的GetEditedExts----------");
        String exts = ConfigManager.GetProperty("files.docservice.edited-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static List<String> GetConvertExts()
    {
    	System.out.println("-----------进入DocumentManeger中的GetConvertExts----------");
        String exts = ConfigManager.GetProperty("files.docservice.convert-docs");
        return Arrays.asList(exts.split("\\|"));
    }

    public static String CurUserHostAddress(String userAddress)
    {
    	System.out.println("-----------进入DocumentManeger中的CurUserHostAddress----------");
        if(userAddress == null)
        {
            try
            {
                userAddress = InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception ex)
            {
                userAddress = "";
            }
        }
        System.out.println("返回结果是："+userAddress.replaceAll("[^0-9a-zA-Z.=]", "_"));
        return userAddress.replaceAll("[^0-9a-zA-Z.=]", "_");
    }

    public static String StoragePath(String fileName, String userAddress)
    {
    	System.out.println("-----------进入DocumentManeger中的StoragePath----------");
        String serverPath = request.getSession().getServletContext().getRealPath("");
        System.out.println("serverPath是："+serverPath);
        String storagePath = ConfigManager.GetProperty("storage-folder");
        System.out.println("storagePath是："+storagePath);
        String hostAddress = CurUserHostAddress(userAddress);
        System.out.println("hostAddress是："+hostAddress);
        String directory = serverPath + File.separator + storagePath + File.separator;
        System.out.println("directory是："+directory);

        File file = new File(directory);

        if (!file.exists())
        {
            file.mkdir();
        }

        directory = directory + hostAddress + File.separator;
        System.out.println("directory是："+directory);
        file = new File(directory);

        if (!file.exists())
        {
            file.mkdir();
        }
        
        System.out.println("返回的是:"+directory + fileName);
        return directory + fileName;
    }

    public static String GetCorrectName(String fileName)
    {
    	System.out.println("-----------进入DocumentManeger中的GetCorrectName----------");
        String baseName = FileUtility.GetFileNameWithoutExtension(fileName);
        String ext = FileUtility.GetFileExtension(fileName);
        String name = baseName + ext;

        File file = new File(StoragePath(name, null));

        for (int i = 1; file.exists(); i++)
        {
            name = baseName + " (" + i + ")" + ext;
            file = new File(StoragePath(name, null));
        }

        return name;
    }

    public static String CreateDemo(String fileExt) throws Exception
    {
    	System.out.println("-----------进入DocumentManeger中的CreateDemo----------");
        String demoName = "sample." + fileExt;
        String fileName = GetCorrectName(demoName);

        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(demoName);

        File file = new File(StoragePath(fileName, null));

        try (FileOutputStream out = new FileOutputStream(file))
        {
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = stream.read(bytes)) != -1)
            {
                out.write(bytes, 0, read);
            }
            out.flush();
        }
        System.out.println("返回fileName："+fileName);
        return fileName;
    }

//    public static String GetFileUri(String fileName)
//    {
//
//        	System.out.println("-----------进入DocumentManeger中的GetFileUri----------");
//            String serverPath = GetServerUrl();
//            System.out.println("serverPath是:"+serverPath);
//            String storagePath = ConfigManager.GetProperty("storage-folder");
//            System.out.println("storagePath是:"+storagePath);
//            String hostAddress = CurUserHostAddress(null);
//            System.out.println("hostAddress是:"+hostAddress);
//            System.out.println(fileName);
//            // 源程序将+转义为 20%
//            String filePath = serverPath + "/" + storagePath + "/" + hostAddress + "/" + fileName;
//            System.out.println("返回的filePath是："+filePath);
//            return filePath;
//
//    }

    public static String GetServerUrl()
    {
    	System.out.println("-----------进入DocumentManeger中的GetServerUrl----------");
//        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    	System.out.println("返回的ServerUrl是:"+request.getScheme() + "://" + "192.168.31.68" + ":" + request.getServerPort() + request.getContextPath());
      return request.getScheme() + "://" + "192.168.31.68" + ":" + request.getServerPort() + request.getContextPath();

    }

    public static String GetCallback(String fileName)
    {
    	System.out.println("-----------进入DocumentManeger中的GetCallback----------");


        String serverPath = GetServerUrl();
        System.out.println("serverPath是："+serverPath);


        String hostAddress = CurUserHostAddress(null);
        System.out.println("hostAddress是："+hostAddress);


        try
        {
//            String query = "?type=track&fileName=" + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString()) + "&userAddress=" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            String query = "?type=track&fileName=" + fileName + "&userAddress=" + URLEncoder.encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString());

        	System.out.println("返回的是"+serverPath + "/IndexServlet" + query);
            return serverPath + "/IndexServlet" + query;
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    public static String GetInternalExtension(FileType fileType)
    {
    	System.out.println("-----------进入DocumentManeger中的GetInternalExtension----------");
        if (fileType.equals(FileType.Text))
            return ".docx";

        if (fileType.equals(FileType.Spreadsheet))
            return ".xlsx";

        if (fileType.equals(FileType.Presentation))
            return ".pptx";

        return ".docx";
    }

    public static String CreateToken(Map<String, Object> payloadClaims)
    {
        try
        {
        	System.out.println("-----------进入DocumentManeger中的CreateToken----------");
            Signer signer = HMACSigner.newSHA256Signer(GetTokenSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet())
            {
                jwt.addClaim(key, payloadClaims.get(key));
            }
            System.out.println("返回的是:"+JWT.getEncoder().encode(jwt, signer));
            return JWT.getEncoder().encode(jwt, signer);
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static JWT ReadToken(String token)
    {
        try
        {
        	System.out.println("-----------进入DocumentManeger中的ReadToken----------");
            Verifier verifier = HMACVerifier.newVerifier(GetTokenSecret());
            return JWT.getDecoder().decode(token, verifier);
        }
        catch (Exception exception)
        {
            return null;
        }
    }

    public static Boolean TokenEnabled()
    {
    	System.out.println("-----------进入DocumentManeger中的TokenEnabled----------");
        String secret = GetTokenSecret();
        System.out.println("test1");
        System.out.println("TokenEnabled返回"+secret != null && !secret.isEmpty());
        System.out.println("test2");
        return secret != null && !secret.isEmpty();
        
    }

    private static String GetTokenSecret()
    {
    	System.out.println("-----------进入DocumentManeger中的GetTokenSecret----------");
        return ConfigManager.GetProperty("files.docservice.secret");
    }
}