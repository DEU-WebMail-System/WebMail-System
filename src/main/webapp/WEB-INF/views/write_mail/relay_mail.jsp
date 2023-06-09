<%-- 
    Document   : relay_mail.jsp
    Author     : 20183109 강상혁
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>

<%-- @taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core" --%>


<html lang="en">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>메일 쓰기 화면</title>
        <link type="text/css" rel="stylesheet" href="css/main_style.css" />
    </head>
    <body>
        <%@include file="../header.jspf"%>

        <div id="sidebar">
            <jsp:include page="../sidebar_previous_menu.jsp" />
        </div>

        <div id="main">
            <%-- <jsp:include page="mail_send_form.jsp" /> --%>
            <form enctype="multipart/form-data" method="POST" action="write_mail.do" >
                <table>
                    <caption>메시지 전달 입력 테이블</caption>
                    <tr>
                        <th scope="col"> 수신 </th>
                        <td> <input type="text" name="to" size="80"> </td>
                    </tr>
                    <tr>
                        <th scope="col">참조</th>
                        <td> <input type="text" id="cc" name="cc" size="80">  </td>
                    </tr>
                    <tr>
                        <th scope="col"> 메일 제목 </th>
                        <td> <input type="text" id="subj" name="subj" size="80" 
                                    value="${!empty param['sender'] ? "전달: " += sessionScope['subject'] : ''}" > </td>
                    </tr>
                    <tr>
                        <th colspan="2">본  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; 문</th>
                    </tr>
                    <tr>  <%-- TextArea    --%>
                        <th colspan="2">
                            <textarea rows="15" id="body" name="body" cols="80">${!empty param['sender'] ? sessionScope['body'] : ''}</textarea> 
                        </th>
                    </tr>
                    <tr>
                        <th scope="col">첨부 파일</th>
                        <td> <input type="file" name="file1"  size="80">  </td>
                    </tr>
                    <tr>
                        <th colspan="2">
                            <input type="submit" value="메일 보내기">
                            <input type="reset" value="다시 입력"> 
                            <button type="button" onclick="doAction()">임시저장 및 뒤로가기</button>
                            <script>
                                function doAction() {
                                    var subject = document.getElementById("subj").value;
                                    var body = document.getElementById("body").value;
                                    var refer = document.getElementById("cc").value;

                                    const link = 'save_temp_mail.do?refer=' + refer + '&subj=' + subject + '&body=' + body;
                                    location.replace(link);
                                }
                            </script>
                        </th>
                    </tr>
                </table>
            </form>
        </div>

        <%@include file="../footer.jspf"%>
    </body>
</html>
