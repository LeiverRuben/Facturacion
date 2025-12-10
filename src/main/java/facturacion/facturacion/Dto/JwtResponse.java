package facturacion.facturacion.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class JwtResponse {
    
    private String token;
    private String username;

}