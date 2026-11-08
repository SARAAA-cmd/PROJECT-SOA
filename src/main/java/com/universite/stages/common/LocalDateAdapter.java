package com.universite.stages.common;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;

/** Sérialisation JAXB des dates au format ISO-8601 (xs:date). */
public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(String v) {
        return v == null || v.isBlank() ? null : LocalDate.parse(v.trim());
    }

    @Override
    public String marshal(LocalDate v) {
        return v == null ? null : v.toString();
    }
}
