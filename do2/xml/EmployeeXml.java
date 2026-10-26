package itsdc.dolphin.do2.xml;

import java.util.ArrayList;
import java.util.List;

public class EmployeeXml {

    private Long employeeId;
    private String armyNo;
    private String rank;
    private String name;

    private List<Do2Detail> gsPayList = new ArrayList<>();
    private List<Do2Detail> upClasList = new ArrayList<>();

    private FamilyMember wife;

    private List<FamilyMember> children = new ArrayList<>();

	public Long getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(Long employeeId) {
		this.employeeId = employeeId;
	}

	public String getArmyNo() {
		return armyNo;
	}

	public void setArmyNo(String armyNo) {
		this.armyNo = armyNo;
	}

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Do2Detail> getGsPayList() {
		return gsPayList;
	}

	public void setGsPayList(List<Do2Detail> gsPayList) {
		this.gsPayList = gsPayList;
	}

	public List<Do2Detail> getUpClasList() {
		return upClasList;
	}

	public void setUpClasList(List<Do2Detail> upClasList) {
		this.upClasList = upClasList;
	}

	public FamilyMember getWife() {
		return wife;
	}

	public void setWife(FamilyMember wife) {
		this.wife = wife;
	}

	public List<FamilyMember> getChildren() {
		return children;
	}

	public void setChildren(List<FamilyMember> children) {
		this.children = children;
	}

     
}
