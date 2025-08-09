package com.bbmovie.payment.service.vnpay;

import lombok.Getter;

@Getter
public enum VnPayOrderType {
    // Food - Consumer Goods
    FOOD_CONSUMER_GOODS("100000", "Thực Phẩm - Tiêu Dùng"),
    CANDY_SNACKS_BEVERAGES("100001", "Bánh kẹo - Đồ ăn vặt - Giải khát"),
    DRIED_FOOD("100003", "Thực phẩm khô"),
    MILK_CREAM_DAIRY_PRODUCTS("100004", "Sữa - Kem & sản phẩm từ sữa"),
    CHEMICALS_CLEANING_AGENTS("100005", "Hóa phẩm – chất tẩy"),

    // Phones - Tablets
    PHONES_TABLETS("110000", "Điện thoại - Máy tính bảng"),
    MOBILE_PHONES("110001", "Điện thoại dị động"),
    TABLETS("110002", "Máy tính bảng"),
    SMART_WATCHES("110003", "Smart Watch"),
    PHONE_ACCESSORIES("110004", "Phụ kiện"),
    SIM_CARDS("110005", "Sim/Thẻ"),

    // Home Appliances
    HOME_APPLIANCES("120000", "Điện gia dụng"),
    KITCHEN_APPLIANCES("120001", "Điện gia dụng nhà bếp"),
    HOUSEHOLD_APPLIANCES("120002", "Điện gia dụng gia đình"),
    REFRIGERATION_LARGE_APPLIANCES("120003", "Điện lạnh & Điện cỡ lớn"),

    // Computers - Office Equipment
    COMPUTERS_OFFICE_EQUIPMENT("130000", "Máy tính - Thiết bị văn phòng"),
    LAPTOPS("130001", "Máy tính xách tay"),
    DESKTOP_COMPUTERS("130002", "Máy tính để bàn"),
    COMPUTER_MONITORS("130003", "Màn hình máy tính"),
    NETWORK_EQUIPMENT("130004", "Thiết bị mạng"),
    SOFTWARE("130005", "Phần mềm"),
    COMPONENTS_ACCESSORIES("130006", "Linh kiện, Phụ kiện"),
    PRINTERS("130007", "Máy in"),
    OTHER_OFFICE_EQUIPMENT("130008", "Thiết bị văn phòng khác"),

    // Electronics - Audio
    ELECTRONICS_AUDIO("140000", "Điện tử - Âm thanh"),
    TV("140001", "Tivi"),
    SPEAKERS("140002", "Loa"),
    SOUND_SYSTEMS("140003", "Dàn âm thanh"),
    TECH_TOYS("140004", "Đồ chơi công nghệ"),
    DIGITAL_DEVICES("140005", "Thiết bị Kỹ thuật số"),

    // Books/Newspapers/Magazines
    BOOKS_NEWSPAPERS_MAGAZINES("150000", "Sách/Báo/Tạp chí"),
    STATIONERY("150001", "Văn phòng phẩm"),
    GIFTS("150002", "Quà tặng"),
    MUSICAL_INSTRUMENTS("150003", "Nhạc cụ"),

    // Sports, Outdoor
    SPORTS_OUTDOOR("160000", "Thể thao, dã ngoại"),
    SPORTS_APPAREL("160001", "Trang phục thể thao"),
    SPORTS_ACCESSORIES("160002", "Phụ kiện thể thao"),
    YOGA_FITNESS_GEAR("160003", "Đồ tập Yoga, thể hình"),
    OUTDOOR_GEAR("160004", "Đồ/Vật dụng Dã ngoại"),

    // Hotels & Travel
    HOTELS_TRAVEL("170000", "Khách sạn & Du lịch"),
    DOMESTIC_TRAVEL("170001", "Du lịch trong nước"),
    INTERNATIONAL_TRAVEL("170002", "Du lịch nước ngoài"),
    HOTEL_BOOKINGS("170003", "Đặt phòng khách sạn"),

    // Food & Drink
    FOOD_DRINK("180000", "Ẩm thực"),

    // Entertainment & Training
    ENTERTAINMENT_TRAINING("190000", "Giải trí & Đào tạo"),
    MOVIE_TICKETS("190001", "Vé xem phim"),
    LEARNING_ONLINE_COURSES("190002", "Thẻ học/ Học trực tuyến"),
    OTHER_ENTERTAINMENT("190003", "Giải trí, vui chơi khác"),
    ONLINE_LEARNING_MEMBERSHIPS("190004", "Thẻ học trực tuyến/Thẻ hội viên"),

    // Fashion
    FASHION("200000", "Thời trang"),
    WOMENS_FASHION("200001", "Thi trang nữ"),
    WOMENS_ACCESSORIES("200002", "Phụ kiện Nữ"),
    MENS_FASHION("200003", "Thi trang Nam"),
    KIDS_FASHION("200004", "Thời trang Trẻ Em"),

    // Health - Beauty
    HEALTH_BEAUTY("210000", "Sức khỏe - Làm đẹp"),
    SUNSCREEN("210001", "Kem chống nắng"),
    FACIAL_SKINCARE("210002", "Chăm sóc da mặt"),
    MAKEUP("210003", "Trang điểm"),
    PERSONAL_CARE("210004", "Chăm sóc cá nhân"),

    // Mother & Baby
    MOTHER_BABY("220000", "Mẹ & Bé"),
    BABY_MILK_POWDER("220001", "Sữa & Bột cho bé"),
    BABY_HYGIENE_CARE("220002", "Vệ sinh chăm sóc cho bé"),
    BABY_TOYS_SUPPLIES("220003", "Đồ chơi & Đồ dùng trẻ em"),
    BABY_FEEDING_SUPPLIES("220004", "Đồ dùng ăn uống cho bé"),

    // Kitchen Utensils
    KITCHEN_UTENSILS("230000", "Vật dụng nhà bếp"),
    FURNITURE("230001", "Nội thất"),

    // Vehicles - Transportation
    VEHICLES_TRANSPORTATION("240000", "Xe cộ - phương tiện"),
    MOTORCYCLES("240001", "Mô tô - Xe máy"),
    MOTORCYCLE_ACCESSORIES("240002", "Phụ kiện xe máy"),
    CAR_ACCESSORIES("240003", "Phụ kiện ô tô"),
    ELECTRIC_BIKES("240004", "Xe đạp điện"),

    // Bill Payment
    BILL_PAYMENT("250000", "Thanh toán hóa đơn"),
    ELECTRICITY_BILLS("250001", "Hóa đơn tiền điện"),
    WATER_BILLS("250002", "Hóa đơn tiền nước"),
    POSTPAID_PHONE_BILLS("250003", "Hóa đơn điện thoại trả sau"),
    ADSL_BILLS("250004", "Hóa đơn ADSL"),
    CABLE_TV_BILLS("250005", "Hóa đơn truyền hình cáp"),
    SERVICE_BILLS("250006", "Hóa đơn dịch vụ"),
    AIR_TICKETS("250007", "Vé máy bay"),

    // Buy Card Codes
    BUY_CARD_CODES("260000", "Mua mã thẻ"),
    PHONE_CARDS("260001", "Thẻ điện thoại"),
    GAME_CARDS("260002", "Thẻ Game"),

    // Pharmacy - Medical Services
    PHARMACY_MEDICAL_SERVICES("270000", "Nhà thuốc - Dich vụ y tế"),
    MEDICAL_EXAM_REGISTRATION("270001", "Đăng ký khám/chữa bệnh");

    private final String code;
    private final String type;

    VnPayOrderType(String code, String type) {
        this.code = code;
        this.type = type;
    }
}