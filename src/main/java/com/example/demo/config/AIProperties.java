package com.example.demo.config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

/*מסמן את המחלקה כמקור של בינס. כך אני אומר לספרינג ליצור טיפוס של המחלקה
ולהכניס כבין לתוך הapplication context כך שיהיה זמין להזרקה למחלקות אחרות
*/
@Configuration
/*זה אומר לספרינג להסתכל לתוך הקובץ קונפיגורציה בשביל תכונות שמתחילות
 * ב "ai" כך אוטומטית מחבר לערכים המתאימים במחלקה*/
@ConfigurationProperties(prefix = "ai")
/*אנוטצית לומבוק שיוצרת אוטומטית קוד שהוא בעל תבנית שחוזרת על עצמה בתכנות
* כגון בנאים getter setters...
* זה חשוב בגלל שספרינג משתמשת בsetters בשביל להזריק את הערכים לתוך השדות מחלקה*/
@Data
public class AIProperties {
    // Spring Boot יזהה אוטומטית את ai.balanced, ai.defensive וכו' ויכניס לפה!
    @NestedConfigurationProperty
    private AIStrategyProps balanced;
    @NestedConfigurationProperty
    private AIStrategyProps defensive;
    @NestedConfigurationProperty
    private AIStrategyProps offensive;
}