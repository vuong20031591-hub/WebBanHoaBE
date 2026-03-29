package com.florastore.web_ban_hoa.config;

import com.florastore.web_ban_hoa.entity.Category;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.entity.Role;
import com.florastore.web_ban_hoa.entity.User;
import com.florastore.web_ban_hoa.repository.CategoryRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import com.florastore.web_ban_hoa.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final String DEFAULT_ADMIN_EMAIL = "admin@florastore.local";

    @Bean
    CommandLineRunner seedDatabase(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        return args -> {
            seedAdminUser(userRepository);

            if (categoryRepository.count() > 0 || productRepository.count() > 0) {
                return;
            }

            Category hoaHong = new Category("Hoa hồng");
            Category hoaCuoi = new Category("Hoa cưới");
            Category hoaSinhNhat = new Category("Hoa sinh nhật");
            Category hoaKhaiTruong = new Category("Hoa khai trương");
            Category hoaChiaBuon = new Category("Hoa chia buồn");

            categoryRepository.saveAll(List.of(hoaHong, hoaCuoi, hoaSinhNhat, hoaKhaiTruong, hoaChiaBuon));

            List<Product> products = new ArrayList<>();
            products.add(new Product("Bó hồng đỏ cổ điển", new BigDecimal("350000"), "Bó 20 bông hồng đỏ tươi", "hong-do-co-dien.jpg", hoaHong));
            products.add(new Product("Giỏ hồng pastel", new BigDecimal("420000"), "Giỏ hoa hồng pastel nhẹ nhàng", "gio-hong-pastel.jpg", hoaHong));
            products.add(new Product("Hộp hồng Ecuador", new BigDecimal("780000"), "Hộp hoa hồng Ecuador sang trọng", "hop-hong-ecuador.jpg", hoaHong));
            products.add(new Product("Kệ hồng chúc mừng", new BigDecimal("1500000"), "Kệ hoa hồng phù hợp khai trương", "ke-hong-chuc-mung.jpg", hoaHong));

            products.add(new Product("Bó cưới tulip trắng", new BigDecimal("890000"), "Bó cưới tulip trắng tinh khôi", "bo-cuoi-tulip-trang.jpg", hoaCuoi));
            products.add(new Product("Bó cưới baby hồng", new BigDecimal("650000"), "Bó cưới baby tone hồng", "bo-cuoi-baby-hong.jpg", hoaCuoi));
            products.add(new Product("Bó cưới lan hồ điệp", new BigDecimal("1250000"), "Bó cưới lan hồ điệp cao cấp", "bo-cuoi-lan-ho-diep.jpg", hoaCuoi));
            products.add(new Product("Set bàn gallery cưới", new BigDecimal("2300000"), "Set hoa trang trí bàn gallery", "set-gallery-cuoi.jpg", hoaCuoi));

            products.add(new Product("Hộp hoa sinh nhật ngọt ngào", new BigDecimal("480000"), "Hộp hoa mix hồng và cẩm chướng", "hop-sinh-nhat-ngot-ngao.jpg", hoaSinhNhat));
            products.add(new Product("Bó hướng dương năng lượng", new BigDecimal("390000"), "Bó hướng dương tươi sáng", "bo-huong-duong-nang-luong.jpg", hoaSinhNhat));
            products.add(new Product("Giỏ hoa sinh nhật sang", new BigDecimal("920000"), "Giỏ hoa cao cấp tặng sinh nhật", "gio-sinh-nhat-sang.jpg", hoaSinhNhat));
            products.add(new Product("Bó cẩm tú cầu pastel", new BigDecimal("560000"), "Bó cẩm tú cầu phối màu pastel", "bo-cam-tu-cau-pastel.jpg", hoaSinhNhat));

            products.add(new Product("Kệ khai trương phát tài", new BigDecimal("1800000"), "Kệ hai tầng tông đỏ vàng", "ke-khai-truong-phat-tai.jpg", hoaKhaiTruong));
            products.add(new Product("Kệ khai trương thịnh vượng", new BigDecimal("2100000"), "Kệ hoa đồng tiền phối lan", "ke-khai-truong-thinh-vuong.jpg", hoaKhaiTruong));
            products.add(new Product("Lẵng hoa chúc mừng đối tác", new BigDecimal("980000"), "Lẵng hoa thanh lịch gửi đối tác", "lang-hoa-chuc-mung.jpg", hoaKhaiTruong));
            products.add(new Product("Kệ mini khai trương", new BigDecimal("750000"), "Kệ mini ngân sách tiết kiệm", "ke-mini-khai-truong.jpg", hoaKhaiTruong));

            products.add(new Product("Vòng hoa chia buồn trang nhã", new BigDecimal("1300000"), "Vòng hoa trắng tím trang nhã", "vong-hoa-chia-buon-trang-nha.jpg", hoaChiaBuon));
            products.add(new Product("Kệ hoa tưởng niệm", new BigDecimal("1650000"), "Kệ hoa tông trắng vàng nhạt", "ke-hoa-tuong-niem.jpg", hoaChiaBuon));
            products.add(new Product("Vòng hoa lan trắng", new BigDecimal("1950000"), "Vòng hoa lan trắng trang trọng", "vong-hoa-lan-trang.jpg", hoaChiaBuon));
            products.add(new Product("Kệ chia buồn đơn giản", new BigDecimal("890000"), "Kệ chia buồn ngân sách vừa", "ke-chia-buon-don-gian.jpg", hoaChiaBuon));

            productRepository.saveAll(products);
        };
    }

    private void seedAdminUser(UserRepository userRepository) {
        if (userRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            return;
        }

        User adminUser = new User(
                DEFAULT_ADMIN_EMAIL,
                "admin123",
                "System Admin",
                "0900000000",
                Role.ADMIN
        );

        userRepository.save(adminUser);
    }
}
