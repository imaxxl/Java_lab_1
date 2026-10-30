package ru.university.socialnetwork.model;

public enum ProfileType {
    PROFILE("Профиль", true),
    DELETED("Удалённый профиль", false),
    COMMUNITY("Сообщество", true);

    private final String label;
    private final boolean editable;

    ProfileType(String label, boolean editable) {
        this.label = label;
        this.editable = editable;
    }

    public boolean isEditableType() { return editable; }

    public static ProfileType of(Profile profile) {
        if (profile instanceof Community) return COMMUNITY;
        if (profile instanceof DeletedProfile) return DELETED;
        return PROFILE;
    }

    @Override
    public String toString() { return label; }
}
