import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JWTTest {

    /****
     * 创建Jwt令牌
     */
    @Test
    public void testCreateJwt(){
        JwtBuilder builder= Jwts.builder()
                .setId("888")             //设置唯一编号
                .setSubject("小白")       //设置主题  可以是JSON数据
                .setIssuedAt(new Date())  //设置签发日期
                .setExpiration(new Date(System.currentTimeMillis()+300000))
                .signWith(SignatureAlgorithm.HS256,"itcast");//设置签名 使用HS256算法，并设置SecretKey(字符串)

        Map<String,Object> map = new HashMap<String, Object>();
        map.put("医生","钟南山");
        builder.addClaims(map);


        //构建 并返回一个字符串
        System.out.println( builder.compact() );
    }


    /***
     * 解析Jwt令牌数据
     */
    @Test
    public void testParseJwt(){
        String compactJwt="eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4ODgiLCJzdWIiOiLlsI_nmb0iLCJpYXQiOjE2MjQ1MzQ0MDgsImV4cCI6MTYyNDUzNDcwOCwi5Yy755SfIjoi6ZKf5Y2X5bGxIn0.t0slpqfhfsrJ27DBNYkCxItnftIkaWSQma5Je59cmHQ";
        Claims claims = Jwts.parser().
                setSigningKey("itcast").
                parseClaimsJws(compactJwt).
                getBody();
        System.out.println(claims);
    }
    
    @Test
    public void test(){
        System.out.println("今天天气不错");
		System.out.println("今天天气不错呀");
    }
}

