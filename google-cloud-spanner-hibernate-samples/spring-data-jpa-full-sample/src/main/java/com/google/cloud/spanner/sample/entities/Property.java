package com.google.cloud.spanner.sample.entities;

import java.util.List;

public class Property {
    private String id;
    private boolean required;
    private boolean disabled;
    private String validation;
    private String default_value;
    private String type;
    private Descriptor descriptor;
    private List<Option> options;
    private DependsOn depends_on;
    private boolean secret;
    private boolean disableOnEdit;
    private Integer minDays;
    private String helperText;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public String getDefault_value() {
        return default_value;
    }

    public void setDefault_value(String default_value) {
        this.default_value = default_value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(Descriptor descriptor) {
        this.descriptor = descriptor;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public DependsOn getDepends_on() {
        return depends_on;
    }

    public void setDepends_on(DependsOn depends_on) {
        this.depends_on = depends_on;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public boolean isDisableOnEdit() {
        return disableOnEdit;
    }

    public void setDisableOnEdit(boolean disableOnEdit) {
        this.disableOnEdit = disableOnEdit;
    }

    public Integer getMinDays() {
        return minDays;
    }

    public void setMinDays(Integer minDays) {
        this.minDays = minDays;
    }

    public String getHelperText() {
        return helperText;
    }

    public void setHelperText(String helperText) {
        this.helperText = helperText;
    }
}