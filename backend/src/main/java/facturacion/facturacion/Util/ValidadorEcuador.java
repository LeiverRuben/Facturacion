package facturacion.facturacion.Util;

public class ValidadorEcuador {

    public static boolean validarRuc(String ruc) {
        if (ruc == null || ruc.length() != 13) {
            return false;
        }
        // Validar que sean números
        if (!ruc.matches("[0-9]+")) {
            return false;
        }

        // Los dos primeros dígitos deben ser válidos (01-24)
        int provincia = Integer.parseInt(ruc.substring(0, 2));
        if (provincia < 1 || provincia > 24) {
            return false;
        }

        // El tercer dígito determina el tipo (0-5: persona, 6: publica, 9: privada)
        int tercerDigito = Integer.parseInt(ruc.substring(2, 3));

        if (tercerDigito < 6) {
            // Persona Natural (sigue algoritmo de cédula + 001)
            return validarCedula(ruc.substring(0, 10)) && ruc.endsWith("001");
        } else if (tercerDigito == 6) {
            // Sociedad Publica (modulo 11, digito verificador en posicion 9)
            return validarPublica(ruc);
        } else if (tercerDigito == 9) {
            // Sociedad Privada (modulo 11, digito verificador en posicion 10)
            return validarPrivada(ruc);
        }

        return false;
    }

    public static boolean validarCedula(String cedula) {
        if (cedula == null || cedula.length() != 10) {
            return false;
        }

        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (provincia < 1 || provincia > 24) {
            return false;
        }

        int[] coeficientes = { 2, 1, 2, 1, 2, 1, 2, 1, 2 };
        int verificador = Integer.parseInt(cedula.substring(9, 10));
        int suma = 0;

        for (int i = 0; i < 9; i++) {
            int valor = Integer.parseInt(cedula.substring(i, i + 1)) * coeficientes[i];
            if (valor >= 10) {
                valor -= 9;
            }
            suma += valor;
        }

        int digitoCalculado = (suma % 10 == 0) ? 0 : (10 - (suma % 10));
        return digitoCalculado == verificador;
    }

    private static boolean validarPublica(String ruc) {
        // Coeficientes: 3, 2, 7, 6, 5, 4, 3, 2
        // Digito verificador posicion 9 (índice 8)
        int verificador = Integer.parseInt(ruc.substring(8, 9));
        int[] coeficientes = { 3, 2, 7, 6, 5, 4, 3, 2 };
        int suma = 0;

        for (int i = 0; i < 8; i++) {
            suma += Integer.parseInt(ruc.substring(i, i + 1)) * coeficientes[i];
        }

        int resto = suma % 11;
        int digitoCalculado = (resto == 0) ? 0 : (11 - resto);

        return digitoCalculado == verificador && ruc.endsWith("0001");
    }

    private static boolean validarPrivada(String ruc) {
        // Coeficientes: 4, 3, 2, 7, 6, 5, 4, 3, 2
        // Digito verificador posicion 10 (índice 9)
        int verificador = Integer.parseInt(ruc.substring(9, 10));
        int[] coeficientes = { 4, 3, 2, 7, 6, 5, 4, 3, 2 };
        int suma = 0;

        for (int i = 0; i < 9; i++) {
            suma += Integer.parseInt(ruc.substring(i, i + 1)) * coeficientes[i];
        }

        int resto = suma % 11;
        int digitoCalculado = (resto == 0) ? 0 : (11 - resto);

        return digitoCalculado == verificador && ruc.endsWith("001");
    }
}
