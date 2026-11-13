# SocialScope

اپلیکیشن مستقل Android برای تحلیل محلی خروجی رسمی اطلاعات Instagram. این برنامه Instagram Mod نیست و برای ورود، رمز عبور یا Session اینستاگرام را دریافت نمی‌کند.

## امکانات این نسخه
- واردکردن فایل ZIP خروجی اطلاعات Instagram (JSON).
- شمارش Followers و Following.
- مقایسه Snapshotهای واردشده برای تشخیص فالوورهای جدید و ازدست‌رفته.
- نمایش افرادی که شما دنبال می‌کنید اما در فهرست Followers فعلی نیستند.
- ذخیره Snapshotها فقط در حافظه خصوصی برنامه.
- دانلود فایل از لینک مستقیم HTTPS با Download Manager اندروید.

## محدودیت‌ها
مشاهده اطلاعات حساب Private بدون اجازه، مخفی‌کردن Seen یا Story View، تغییر وضعیت Online در سمت سرور و حذف تبلیغات Instagram از طریق این اپ مستقل ممکن نیست.

## دریافت خروجی
در GitHub به بخش Actions برو، اجرای آخر workflow با نام Build Android APK را باز کن و از قسمت Artifacts فایل SocialScope-debug-apk را دریافت کن. این خروجی Debug است و برای انتشار عمومی امضای Release جداگانه لازم دارد.

## ورود داده
از Instagram بخش Accounts Center > Your information and permissions > Download your information را باز کن. داده‌های Followers and following را با فرمت JSON و ZIP بگیر و در برنامه وارد کن.
