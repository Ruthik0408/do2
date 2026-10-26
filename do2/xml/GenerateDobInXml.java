package itsdc.dolphin.do2.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import itsdc.dolphin.repositories.Do2Repository;

@Service
public class GenerateDobInXml {

    @Autowired
    private Do2Repository do2Repo;

    @Autowired
    private Environment env;

    public String generateDobInXml() throws Exception {

        List<ChildDobDto> rows = do2Repo.findDobForEachEmployeeForCea();

        StringWriter sw = new StringWriter();

        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter xml = factory.createXMLStreamWriter(sw);

        xml.writeStartDocument("UTF-8", "1.0");

        xml.writeStartElement("Employees");

        Map<String, List<ChildDobDto>> grouped =
                rows.stream()
                        .collect(Collectors.groupingBy(
                                ChildDobDto::getArmyNo,
                                LinkedHashMap::new,
                                Collectors.toList()));

        DateTimeFormatter output =
                DateTimeFormatter.ofPattern("dd-MM-yyyy");

        for (List<ChildDobDto> employeeRows : grouped.values()) {

            ChildDobDto first = employeeRows.get(0);

            xml.writeStartElement("Employee");

            // Army No
            xml.writeStartElement("army_no");
            xml.writeCharacters(
                    first.getArmyNo() == null ? "" : first.getArmyNo());
            xml.writeEndElement();

            // Employee Name
            xml.writeStartElement("name");
            xml.writeCharacters(
                    first.getName() == null ? "" : first.getName());
            xml.writeEndElement();

            for (ChildDobDto row : employeeRows) {

                LocalDate dob = parseDob(row.getDob());

                xml.writeStartElement("Child");

                // Child Name
                xml.writeStartElement("name_of_member");
                xml.writeCharacters(
                        row.getNameOfMember() == null
                                ? ""
                                : row.getNameOfMember());
                xml.writeEndElement();

                // Relationship
                xml.writeStartElement("relationship");
                xml.writeCharacters(
                        row.getRelationship() == null
                                ? ""
                                : row.getRelationship());
                xml.writeEndElement();

                // DOB
                xml.writeStartElement("date_of_birth");

                if (dob != null) {
                    xml.writeCharacters(dob.format(output));
                } else {
                    xml.writeCharacters("INVALID_DOB");
                }

                xml.writeEndElement();

                xml.writeEndElement(); // Child
            }

            xml.writeEndElement(); // Employee
        }

        xml.writeEndElement(); // Employees
        xml.writeEndDocument();

        xml.flush();
        xml.close();

        String xmlString = sw.toString();

        String reportDir =
                env.getProperty("spring.application.filedir")
                        + "/reports";

        File dir = new File(reportDir);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName =
                "ChildDOBReport_"
                        + new SimpleDateFormat("ddMMyyyyHHmmss")
                                .format(new Date())
                        + ".xml";

        File xmlFile = new File(dir, fileName);

        try (FileWriter fw = new FileWriter(xmlFile)) {
            fw.write(xmlString);
        }

        System.out.println(
                "Child DOB XML generated : "
                        + xmlFile.getAbsolutePath());

        return xmlFile.getAbsolutePath();
    }

    private LocalDate parseDob(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        value = value.trim();

        if (!value.matches("\\d{8}")) {
            return null;
        }

        if ("00000000".equals(value)) {
            return null;
        }

        // Try YYYYMMDD
        try {

            int year =
                    Integer.parseInt(value.substring(0, 4));

            if (year >= 1900 && year <= 2100) {

                return LocalDate.parse(
                        value,
                        DateTimeFormatter.ofPattern("uuuuMMdd")
                                .withResolverStyle(
                                        ResolverStyle.STRICT));
            }

        } catch (Exception ignored) {
        }

        // Try DDMMYYYY
        try {

            int year =
                    Integer.parseInt(value.substring(4, 8));

            if (year >= 1900 && year <= 2100) {

                return LocalDate.parse(
                        value,
                        DateTimeFormatter.ofPattern("ddMMuuuu")
                                .withResolverStyle(
                                        ResolverStyle.STRICT));
            }

        } catch (Exception ignored) {
        }

        return null;
    }
}