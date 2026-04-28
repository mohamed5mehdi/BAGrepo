package com.pfe.gestionsachat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.gestionsachat.model.*;

public class TestJson {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        
        User d = new User();
        d.setOidUser(1);
        d.setNom("Test");
        
        DaHeader da = new DaHeader();
        da.setOidDa(10);
        da.setDemandeur(d);
        da.setStatut(StatutDA.EN_ATTENTE_N1);
        
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(da));
    }
}
