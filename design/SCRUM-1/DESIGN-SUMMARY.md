# SCRUM-1 – "Welcome back" login screen: design summary

Source: the Jira attachment `signin-screenshot.png` (650×1452 px), because the Stitch project
4585550613556754587 couldn't be accessed from the Stitch MCP account. The colours below were
sampled from the screenshot's pixels. Sizes were converted at about 1.67 px per dp (frame ≈ 390 dp
wide) and rounded to a 4 dp grid, so they are approximate. Wherever the ticket's accessibility
criteria require something larger (48 dp touch targets, AA contrast), the ticket wins over the design.

## Colours (dark theme only)
| Token | Hex | Used for |
|---|---|---|
| background | #11141E | Screen background, with a faint indigo radial glow behind the top icon |
| surfaceHigh | #272A33 | Input fields, icon tile |
| surfaceMid | #1E2129 | Disabled Log in button |
| surfaceLow | #191B24 | Passkey pill, Google/Apple buttons |
| outline | #32353E | Checkbox box, divider lines |
| textPrimary | #E0E2EE | Heading, social button labels |
| textSecondary | #C6C4D6 | Subtitle, field labels, "Remember me", passkey label, "Don't have an account?" |
| textTertiary | #918FA0 | Field leading icons, eye icon, "Or continue with" |
| primary | #C2C0FA | Shield icon, "Forgot password?", "Sign up", link icon, enabled button accent |
| placeholder (design) | #464554 | Too low-contrast: **use textTertiary #918FA0 instead** (AC: WCAG AA) |
| disabled button text (design) | #5D5D6B | Too low-contrast: **use at least #8A8A99 on #1E2129** (AC) |
| enabled Log in button | primary #C2C0FA background, #1B1A3A text | Not shown in the design; derived from primary |
| error | #FFB4AB | Not shown in the design; Material dark error |

## Typography (system sans, e.g. Inter/Roboto)
- Heading "Welcome back": 28 sp, Bold, textPrimary, centred
- Subtitle "Enter your credentials to access your secure account": 16 sp, Regular, textSecondary, centred, 24 sp line height
- Field labels "Email address", "Password": 14 sp, Medium, textSecondary
- Input text and placeholders: 16 sp, Regular
- "Remember me": 16 sp; "Forgot password?": 14 sp, Medium, primary
- "Log in" button: 16 sp, SemiBold, with a trailing arrow → icon
- "Quick log in with Passkey": 14 sp, Medium
- "Or continue with": 14 sp, textTertiary
- "Google" / "Apple": 16 sp, Medium, textPrimary
- "Don't have an account?" 16 sp textSecondary + "Sign up" 16 sp Bold primary + external-link icon (14 dp)

## Layout (top to bottom), horizontal padding 24 dp, respect safe areas / insets, scrollable
1. Icon tile 56×56 dp, radius 16 dp, surfaceHigh, with a soft indigo glow; shield-with-lock icon 24 dp, primary. Top margin about 16 dp.
2. 20 dp gap → Heading → 8 dp → Subtitle
3. 32 dp gap → "Email address" label → 8 dp → email field
4. 16 dp gap → "Password" label → 8 dp → password field
   - Fields: height 52 dp, radius 12 dp, surfaceHigh fill, no visible border (add a primary 1 dp border on focus and an error border on error); leading icon 20 dp (mail / lock), 16 dp start padding, 12 dp gap to the text; password field has a trailing eye icon 24 dp (eye / eye-off)
   - Email placeholder `name@example.com`; password placeholder `Enter your password`
5. 20 dp gap → row: checkbox (20 dp box, radius 4 dp, outline fill) + "Remember me" on the left; "Forgot password?" on the right
6. 24 dp gap → "Log in →" button, full width, 52 dp, radius 12 dp (disabled = surfaceMid)
7. 12 dp gap → "Quick log in with Passkey" pill, centred, wraps its content, radius 24 dp (fully rounded), surfaceLow, fingerprint icon 18 dp, height at least 48 dp
8. 24 dp gap → divider row: 1 dp outline lines on both sides of "Or continue with"
9. 24 dp gap → two equal-width buttons with a 12 dp gap: "G Google" (multicolour G logo) and " Apple" (white Apple logo); 48 dp tall, radius 12 dp, surfaceLow
10. 48 dp gap → centred "Don't have an account? Sign up ↗"

## Icons
Shield/lock, mail, lock, eye/eye-off, arrow-forward, fingerprint, external-link (Material Symbols Outlined or equivalent vectors); the Google "G" multicolour logo and the Apple logo as vector drawables.
