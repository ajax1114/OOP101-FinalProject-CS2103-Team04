package com.dungeonbound.game;

/**
 * Centralized localization dictionary according to user specifications.
 */
public final class Strings {

    private Strings() {}

    // 1. Title Screen
    public static String titlePrompt() {
        return Lang.isTL()
                ? "PINDUTIN PARA MAGSIMULA"
                : "PRESS ANY KEY / TAP TO START";
    }

    // 2. Login Screen
    public static String loginBtn() {
        return Lang.isTL() ? "MAG-LOGIN" : "LOGIN";
    }

    public static String loggingIn() {
        return Lang.isTL() ? "Nagla-log in..." : "Logging in...";
    }

    public static String signUpBtn() {
        return Lang.isTL() ? "MAGPATALA" : "SIGN UP";
    }

    public static String usernamePlaceholder() {
        return Lang.isTL() ? "Pangalan ng User" : "Username";
    }

    public static String passwordPlaceholder() {
        return Lang.isTL() ? "Password" : "Password";
    }

    public static String errInvalidLogin() {
        return Lang.isTL() ? "Maling pangalan ng user o password." : "Invalid username or password.";
    }

    public static String errFillAll() {
        return Lang.isTL() ? "Pakiusap punan ang lahat ng patlang." : "Please fill in all fields.";
    }

    public static String rememberMe() {
        return Lang.isTL() ? "TANDAAN AKO" : "REMEMBER ME";
    }

    public static String forgotPassword() {
        return Lang.isTL() ? "Nakalimutan ang password?" : "Forgot password?";
    }

    // 3. Sign Up Screen
    public static String lblUsername() {
        return Lang.isTL() ? "PANGALAN" : "USERNAME";
    }

    public static String lblPassword() {
        return Lang.isTL() ? "PASSWORD" : "PASSWORD";
    }

    public static String lblConfirmPassword() {
        return Lang.isTL() ? "ULITIN ANG PASSWORD" : "CONFIRM PASSWORD";
    }

    public static String signingUp() {
        return Lang.isTL() ? "Gumagawa ng Account..." : "Signing up...";
    }

    public static String errPasswordsNotMatch() {
        return Lang.isTL() ? "Hindi magkatugma ang password." : "Passwords do not match.";
    }

    public static String errUserExists() {
        return Lang.isTL() ? "May gumagamit na ng username na ito." : "Username already exists.";
    }

    // 4. Start Menu Screen
    public static String menuPlay() {
        return Lang.isTL() ? "SIMULAN ANG LARO" : "PLAY";
    }

    public static String menuBestiary() {
        return Lang.isTL() ? "TALAAN NG NILALANG" : "CREATURE LORE";
    }

    public static String menuSettings() {
        return Lang.isTL() ? "MGA SETTING" : "SETTINGS";
    }

    public static String menuLogout() {
        return Lang.isTL() ? "MAG-LOGOUT" : "LOGOUT";
    }

    // 5. Lore Screen
    public static String btnBack() {
        return Lang.isTL() ? "BUMALIK" : "BACK";
    }

    // 6. Loading Screen
    public static String loading() {
        return Lang.isTL() ? "Sandali lamang..." : "Loading...";
    }
}
