# Implementation Plan - AI Subscription Integration

This plan integrates a Google Play Billing-based subscription feature into the "GOI Retrieval" app. The subscription page will appear automatically after a successful login or 2FA verification.

## User Review Required

> [!IMPORTANT]
> The provided code uses a hardcoded price of **₹100/Month**. Please confirm if this is the correct pricing model you'd like to display in the UI. 
> 
> Also, I have set the subscription SKU to `nutriai_premium_subscription` as per your provided code, but you'll need to ensure this matches exactly what you configure in the Google Play Console.

## Proposed Changes

### Dependencies

#### [MODIFY] [build.gradle.kts](file:///c:/Users/pokal/AndroidStudioProjects/GOI_Retrieval/app/build.gradle.kts)
- Add `implementation("com.android.billingclient:billing-ktx:7.0.0")` to support Google Play Billing.

---

### New Components

#### [NEW] [SubscriptionActivity.kt](file:///c:/Users/pokal/AndroidStudioProjects/GOI_Retrieval/app/src/main/java/com/simats/goiretrieval/SubscriptionActivity.kt)
- Re-implementation of the provided subscription logic.
- Updated package to `com.simats.goiretrieval`.
- Updated navigation: Skipping or completing subscription will lead to `MainActivity.kt`.
- Includes `BillingClient` setup, product querying, and purchase handling.

#### [NEW] [activity_subscription.xml](file:///c:/Users/pokal/AndroidStudioProjects/GOI_Retrieval/app/src/main/res/layout/activity_subscription.xml)
- Re-implementation of the provided premium UI.
- Branded as **GOI Retrieval Premium**.
- Updated image references to use the existing `@mipmap/ic_launcher_premium`.
- Uses a sleek dark theme as provided.

---

### Security & 2FA Synchronization

#### [MODIFY] [Models.kt](file:///c:/Users/pokal/AndroidStudioProjects/GOI_Retrieval/app/src/main/java/com/simats/goiretrieval/api/Models.kt)
- Add `@SerializedName("is_2fa_enabled") val is2faEnabled: Boolean? = false` to the `User` and `SignupResponse` data classes.
- This allows the app to know if 2FA is active even during a "success" or "2fa_required" response.

#### [MODIFY] [SignInActivity.kt](file:///c:/Users/pokal/AndroidStudioProjects/GOI_Retrieval/app/src/main/java/com/simats/goiretrieval/SignInActivity.kt)
- Update login logic:
    - If `status == "2fa_required"`, proceed to `TwoFactorVerifyActivity`.
    - If `status == "success"`, save the session and proceed directly to `SubscriptionActivity`.
- This ensures 2FA is skipped entirely if the backend indicates the user is fully authenticated.

## Open Questions

- Should we check for an existing subscription on every app launch (in `SplashActivity`), or is after-login sufficient for now? (Proposed: Keep as after-login for this phase).
- Do you have a specific icon for "GOI Retrieval Premium" or should I use the standard app icon? (Proposed: Use `ic_launcher_premium`).

## Verification Plan

### Manual Verification
1. **2FA Disabled**: Log in with an account that has 2FA turned OFF. Verify it goes straight to the **Subscription** page.
2. **2FA Enabled**: Log in with an account that has 2FA turned ON. Verify it goes to **2FA Verification**, then to **Subscription**.
3. **Registration**: Register a new user and ensure they follow the standard flow.

