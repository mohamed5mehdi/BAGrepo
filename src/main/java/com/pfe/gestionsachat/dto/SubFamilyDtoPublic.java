package com.pfe.gestionsachat.dto;

public class SubFamilyDtoPublic {
    private Integer id;
    private String name;
    private Integer familyId;

    public SubFamilyDtoPublic(Integer id, String name, Integer familyId) {
        this.id = id;
        this.name = name;
        this.familyId = familyId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getFamilyId() { return familyId; }
    public void setFamilyId(Integer familyId) { this.familyId = familyId; }
}
