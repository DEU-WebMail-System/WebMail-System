/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package deu.cse.spring_webmail.secure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

/**
 *
 * @author joon
 */
@Configuration
@Slf4j
public class JasyptConfig {

    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() throws FileNotFoundException, IOException {

        log.debug("config is 호출");
//        BufferedReader br = new BufferedReader(new FileReader("./key.txt"));
//        String key = br.readLine();
//        br.close();
//       
        ClassPathResource resource = new ClassPathResource("key.txt");
        byte[] bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String key = new String(bdata, StandardCharsets.UTF_8); 
//        Path path = Paths.get(resource.getURI());
//        String key = Files.readString(path);

        log.debug("[TEST] key : {}", key);
        //key 암호화  
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(key); // 암호화할 때 사용하는 키
        config.setAlgorithm("PBEWithMD5AndDES"); // 대칭키 암호화 알고리즘 - MD5와DES를 합친 암호화
        config.setKeyObtentionIterations("1000"); // 반복할 해싱 회수
        config.setPoolSize("1"); // 인스턴스 pool
        config.setProviderName("SunJCE"); //프로바이더
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator"); // salt 생성 클래스
        config.setStringOutputType("base64"); //인코딩 방식

        encryptor.setConfig(config);
        return encryptor;
    }

}
