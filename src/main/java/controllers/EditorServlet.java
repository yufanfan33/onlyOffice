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

import helpers.ConfigManager;
import helpers.DocumentManager;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import entities.FileModel;


@WebServlet(name = "EditorServlet", urlPatterns = {"/EditorServlet"})
public class EditorServlet extends HttpServlet
{
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {

    	
        DocumentManager.Init(request, response);
        System.out.println("=======================");
        System.out.println(request.getCharacterEncoding());
        request.setCharacterEncoding("UTF-8");
        
        String fileName = null;
        //定义文档路径
        String URL = null;
        //String fileName = new String(request.getParameter("fileName"));
        //System.out.println("request里面的fileName:"+fileName);
        String fileExt = request.getParameter("fileExt");

        if (fileExt != null && null == request.getParameter("fileName"))
        {
            try{
//                fileName = DocumentManager.CreateDemo(fileExt);
                //首次访问的时候需要创建文件，上传到minmio服务器
//                URL = "http://106.13.57.201:9000/test/202011/sample.docx?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=admin%2F20201121%2F%2Fs3%2Faws4_request&X-Amz-Date=20201121T114449Z&X-Amz-Expires=432000&X-Amz-SignedHeaders=host&X-Amz-Signature=317b40fb732275072f4fe3ceed5548a147c98132c3da32c751961bff97d750e1";
                fileName= "/test/202011/sample.docx";
            }
            catch (Exception ex) {
                response.getWriter().write("Error: " + ex.getMessage());    
            }
        }
        else {
        	fileName = new String(request.getParameter("fileName"));
        	//根据文件路径查询出fileName
        }

        FileModel file = new FileModel(fileName);
        if ("embedded".equals(request.getParameter("mode")))
            file.InitDesktop();
        if ("view".equals(request.getParameter("mode")))
            file.editorConfig.mode = "view";

        if (DocumentManager.TokenEnabled())
        {
            file.BuildToken();
        }

        request.setAttribute("file", file);
        request.setAttribute("docserviceApiUrl", ConfigManager.GetProperty("files.docservice.url.api"));
        request.getRequestDispatcher("editor.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo()
    {
        return "Editor page";
    }
}
