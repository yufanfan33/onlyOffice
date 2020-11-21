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


package entities;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import helpers.DocumentManager;
import helpers.ServiceConverter;
import helpers.FileUtility;
import com.google.gson.Gson;

public class FileModel
{
    public String type = "desktop";
    public String documentType;
    public Document document;
    public EditorConfig editorConfig;
    public String token;
    

    public FileModel(String fileName) throws UnsupportedEncodingException
    {
    	System.out.println("----------进入FileModel----------");
        if (fileName == null) fileName = "";
        fileName = fileName.trim();

        documentType = FileUtility.GetFileType(fileName).toString().toLowerCase();

        document = new Document();
        document.title = fileName;
        System.out.println("FileModel传过去的fileName是："+fileName);
        
        //TODO

//        根据name获取minio
//        document.url =DocumentManager.GetFileUri(fileName) ;
        document.url = "http://106.13.57.201:9000/test/202011/sample.docx?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20201121%2F%2Fs3%2Faws4_request&X-Amz-Date=20201121T120302Z&X-Amz-Expires=432000&X-Amz-SignedHeaders=host&X-Amz-Signature=5d5e2a91f86ba205190f8e8e2a8888fe9f35fad8ae5875a766aa1b107d190022";

        System.out.println("document.url:"+document.url);

        document.fileType = FileUtility.GetFileExtension(fileName).replace(".", "");
        String userId = DocumentManager.CurUserHostAddress(null);
        document.key = ServiceConverter.GenerateRevisionId(userId + "/" + fileName);

        editorConfig = new EditorConfig();
        if (!DocumentManager.GetEditedExts().contains(FileUtility.GetFileExtension(fileName)))
            editorConfig.mode = "view";
        editorConfig.callbackUrl = DocumentManager.GetCallback(fileName);
        System.out.println("callbackUrl是："+editorConfig.callbackUrl);
        editorConfig.user.id = userId;

        editorConfig.customization.goback.url = DocumentManager.GetServerUrl() + "/IndexServlet";
    }

    public void InitDesktop()
    {
        type = "embedded";
        editorConfig.InitDesktop(document.url);
    }

    public void BuildToken()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);

        token = DocumentManager.CreateToken(map);
    }

    public class Document
    {
        public String title;
        public String url;
        public String fileType;
        public String key;
    }

    public class EditorConfig
    {
        public String mode = "edit";
        public String callbackUrl;
        public User user;
        public Customization customization;
        public Embedded embedded;

        public EditorConfig()
        {
            user = new User();
            customization = new Customization();
        }

        public void InitDesktop(String url)
        {
            embedded = new Embedded();
            embedded.saveUrl = url;
            embedded.embedUrl = url;
            embedded.shareUrl = url;
            embedded.toolbarDocked = "top";
        }

        public class User
        {
            public String id;
            public String name = "John Smith";
        }

        public class Customization
        {
            public Goback goback;

            public Customization()
            {
                goback = new Goback();
            }

            public class Goback
            {
                public String url;
            }
        }

        public class Embedded
        {
            public String saveUrl;
            public String embedUrl;
            public String shareUrl;
            public String toolbarDocked;
        }
    }


    public static String Serialize(FileModel model)
    {
        Gson gson = new Gson();
        return gson.toJson(model);
    }
}
