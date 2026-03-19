package Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
public class User
{
    @Id @Setter
    @Getter
    private Long id;
    @Getter @Setter
    private String username;
    @Getter @Setter
    private String password;


}
