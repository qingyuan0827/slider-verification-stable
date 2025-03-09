package chuanglan;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;

@Slf4j
public class APIClient {

    private static final String URL_GET_NUMBER = "http://www.jisu366.com/jk/getnumber";
    private static final String URL_GET_CODE = "http://www.jisu366.com/jk/getcode";
    private static final String API_KEY = "AzjteYd3cJ";
    private static final String PID = "323";
    private static final String QUHAO = "855";
    private Integer id;

    public static void main(String[] args) {
        try {
            APIClient client = new APIClient();
            String response = client.fetchNumber();
            if (response != null) {
                JSONObject jsonResponse = new JSONObject(response);
                int errno = jsonResponse.getInt("errno");
                if (errno == 0) {
                    JSONObject ret = jsonResponse.getJSONObject("ret");
                    String number = ret.getString("number");
                    client.id = ret.getInt("qhid");
                    log.info("获取到的号码: " + number + "，取号id: " + client.id);

                    // 保存结果到文件
                    Path filePath = Paths.get("src", "main", "resources", "number_result.json");
                    Files.writeString(filePath, jsonResponse.toString(2));
                    log.info("结果已保存到: " + filePath.toAbsolutePath());

                    log.info(client.getVerifyCode().getValue());
                } else {
                    String errmsg = jsonResponse.getString("errmsg");
                    log.error("请求失败: " + errmsg);
                }
            }
        } catch (Exception e) {
            log.error("请求过程中发生错误: ", e);
        }
    }

    public String fetchNumber() throws Exception {
        String number = null;
        String response = httpGet(URL_GET_NUMBER + "?apikey=" + API_KEY + "&pid=" + PID + "&quhao=" + QUHAO);
        if (response != null) {
            JSONObject jsonResponse = new JSONObject(response);
            int errno = jsonResponse.getInt("errno");
            if (errno == 0) {
                JSONObject ret = jsonResponse.getJSONObject("ret");
                number = ret.getString("number");
                this.id = ret.getInt("qhid");
                log.info("获取到的号码: " + number + "，取号id: " + this.id);

                // 保存结果到文件
//                   Path filePath = Paths.get("src", "main", "resources", "number_result.json");
//                   Files.writeString(filePath, jsonResponse.toString(2));
//                   log.info("结果已保存到: " + filePath.toAbsolutePath());
            } else {
                String errmsg = jsonResponse.getString("errmsg");
                log.error("请求失败: " + errmsg);
            }
        }
        return number;
    }

    public String fetchWithExistedNumber(String number) throws Exception {
        String response = httpGet(URL_GET_NUMBER + "?apikey=" + API_KEY + "&pid=" + PID
                + "&quhao=" + QUHAO + "&number=" + number);
        if (response != null) {
            JSONObject jsonResponse = new JSONObject(response);
            int errno = jsonResponse.getInt("errno");
            if (errno == 0) {
                JSONObject ret = jsonResponse.getJSONObject("ret");
                number = ret.getString("number");
                this.id = ret.getInt("qhid");
                log.info("获取到的号码: " + number + "，取号id: " + this.id);

                // 保存结果到文件
                Path filePath = Paths.get("src", "main", "resources", "number_result.json");
                Files.writeString(filePath, jsonResponse.toString(2));
                log.info("结果已保存到: " + filePath.toAbsolutePath());
            } else {
                String errmsg = jsonResponse.getString("errmsg");
                log.error("请求失败: " + errmsg);
            }
        }
        return number;
    }

    public Map.Entry<Integer, String> getVerifyCode() throws Exception {
        int errno = 0;
        String verifyMsg = null;
        String response = httpGet(URL_GET_CODE + "?apikey=" + API_KEY + "&qhid=" + id);
        log.info(response);
        if (response != null) {
            JSONObject jsonResponse = new JSONObject(response);
            errno = jsonResponse.getInt("errno");
            if (errno == 0) {
                JSONObject ret = jsonResponse.getJSONObject("ret");
                verifyMsg = ret.getString("cnt");
            } else {
                String errmsg = jsonResponse.getString("errmsg");
                log.error("请求失败: " + errmsg);
            }
        }
        return new AbstractMap.SimpleEntry<>(errno, verifyMsg);
    }

    public String httpGet(String url) throws Exception {
        log.info("请求地址：" + url);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // 设置连接超时时间为10秒
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .timeout(Duration.ofSeconds(30)) // 设置请求超时时间为30秒
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            log.error("请求失败，状态码: " + response.statusCode());
            return null;
        }
    }
}
   