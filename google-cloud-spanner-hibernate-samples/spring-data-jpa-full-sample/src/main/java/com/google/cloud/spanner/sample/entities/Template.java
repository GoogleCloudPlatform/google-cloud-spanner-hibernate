package com.google.cloud.spanner.sample.entities;

public class Template {
    private String id;
    private String vendor;
    private String acc;
    private String schemaVersion;
    private String icon;
    private String type;
    private Descriptor descriptor;
    private DetailTemplate detailTemplate;
    private String accountId;
    private Boolean hasAccount;
    private Boolean canTestConnection;
    private String dataSourceType;
    private Boolean runNowSupported;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getAcc() {
        return acc;
    }

    public void setAcc(String acc) {
        this.acc = acc;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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

    public DetailTemplate getDetailTemplate() {
        return detailTemplate;
    }

    public void setDetailTemplate(DetailTemplate detailTemplate) {
        this.detailTemplate = detailTemplate;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Boolean getHasAccount() {
        return hasAccount;
    }

    public void setHasAccount(Boolean hasAccount) {
        this.hasAccount = hasAccount;
    }

    public Boolean getCanTestConnection() {
        return canTestConnection;
    }

    public void setCanTestConnection(Boolean canTestConnection) {
        this.canTestConnection = canTestConnection;
    }

    public String getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public Boolean getRunNowSupported() {
        return runNowSupported;
    }

    public void setRunNowSupported(Boolean runNowSupported) {
        this.runNowSupported = runNowSupported;
    }
}