package com.florastore.web_ban_hoa.config;

import com.florastore.web_ban_hoa.entity.Category;
import com.florastore.web_ban_hoa.entity.Product;
import com.florastore.web_ban_hoa.repository.CategoryRepository;
import com.florastore.web_ban_hoa.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDatabase(CategoryRepository categoryRepository, ProductRepository productRepository) {
        return args -> {
            if (categoryRepository.count() > 0 || productRepository.count() > 0) {
                return;
            }

            Category hoaHong = new Category("Hoa hồng");
            Category hoaCuoi = new Category("Hoa cưới");
            Category hoaSinhNhat = new Category("Hoa sinh nhật");
            Category hoaKhaiTruong = new Category("Hoa khai trương");
            Category hoaChiaBuon = new Category("Hoa chia buồn");

            // Add descriptions for categories
            hoaHong.setDescription("Các sản phẩm bó, giỏ, hộp hoa hồng cho mọi dịp.");
            hoaCuoi.setDescription("Bó và trang trí hoa dành cho đám cưới, cô dâu và tiệc cưới.");
            hoaSinhNhat.setDescription("Hoa tặng sinh nhật: bó, hộp và giỏ đa dạng.");
            hoaKhaiTruong.setDescription("Kệ và lẵng hoa chúc mừng khai trương, tân gia, sự kiện.");
            hoaChiaBuon.setDescription("Sản phẩm vòng và kệ chia buồn, trang trọng và lịch sự.");

            categoryRepository.saveAll(List.of(hoaHong, hoaCuoi, hoaSinhNhat, hoaKhaiTruong, hoaChiaBuon));

            List<Product> products = new ArrayList<>();
            products.add(new Product("Bó hồng đỏ cổ điển - 20 bông", new BigDecimal("350000"), "Bó gồm 20 bông hồng đỏ tươi, gói giấy kraft, ruy băng sang trọng.", "hong-do-co-dien.jpg", hoaHong));
            products.add(new Product("Giỏ hồng pastel - tông nhẹ", new BigDecimal("420000"), "Giỏ hoa phối màu pastel gồm hồng và baby, phù hợp tặng dịu dàng.", "gio-hong-pastel.jpg", hoaHong));
            products.add(new Product("Hộp hồng Ecuador - sang trọng", new BigDecimal("780000"), "Hộp hoa hồng Ecuador nhập khẩu, đóng hộp quà premium.", "hop-hong-ecuador.jpg", hoaHong));
            products.add(new Product("Kệ hồng chúc mừng - cao cấp", new BigDecimal("1500000"), "Kệ hoa nhiều tầng, sử dụng hồng phối lá xanh, thích hợp chúc mừng.", "ke-hong-chuc-mung.jpg", hoaHong));

            products.add(new Product("Bó cưới tulip trắng - elegant", new BigDecimal("890000"), "Bó tulip trắng tinh khôi, phù hợp cô dâu hoặc trang trí tiệm cưới.", "bo-cuoi-tulip-trang.jpg", hoaCuoi));
            products.add(new Product("Bó cưới baby hồng - nhẹ nhàng", new BigDecimal("650000"), "Bó hoa baby và hồng tone hồng nhạt, phong cách romantic.", "bo-cuoi-baby-hong.jpg", hoaCuoi));
            products.add(new Product("Bó cưới lan hồ điệp - cao cấp", new BigDecimal("1250000"), "Lan hồ điệp trắng gắn kết thành bó cưới sang trọng, tôn dáng cô dâu.", "bo-cuoi-lan-ho-diep.jpg", hoaCuoi));
            products.add(new Product("Set bàn gallery cưới - full", new BigDecimal("2300000"), "Gói trang trí bàn gallery gồm nhiều phụ kiện hoa tươi, phù hợp sự kiện.", "set-gallery-cuoi.jpg", hoaCuoi));

            products.add(new Product("Hộp hoa sinh nhật ngọt ngào", new BigDecimal("480000"), "Hộp mix hồng và cẩm chướng, tông màu ấm, kèm thiệp chúc mừng.", "hop-sinh-nhat-ngot-ngao.jpg", hoaSinhNhat));
            products.add(new Product("Bó hướng dương năng lượng", new BigDecimal("390000"), "Bó hướng dương rực rỡ, gửi tặng truyền cảm hứng và năng lượng.", "bo-huong-duong-nang-luong.jpg", hoaSinhNhat));
            products.add(new Product("Giỏ hoa sinh nhật sang trọng", new BigDecimal("920000"), "Giỏ hoa cao cấp gồm nhiều loài phối hợp, dành cho dịp đặc biệt.", "gio-sinh-nhat-sang.jpg", hoaSinhNhat));
            products.add(new Product("Bó cẩm tú cầu pastel - nhẹ nhàng", new BigDecimal("560000"), "Bó cẩm tú cầu phối màu pastel, phong cách tinh tế, đáng yêu.", "bo-cam-tu-cau-pastel.jpg", hoaSinhNhat));

            products.add(new Product("Kệ khai trương phát tài", new BigDecimal("1800000"), "Kệ hoa lớn tone đỏ vàng, biểu tượng phát tài cho khai trương.", "ke-khai-truong-phat-tai.jpg", hoaKhaiTruong));
            products.add(new Product("Kệ khai trương thịnh vượng", new BigDecimal("2100000"), "Kệ hoa phối đồng tiền và lan, phù hợp sự kiện khai trương quy mô.", "ke-khai-truong-thinh-vuong.jpg", hoaKhaiTruong));
            products.add(new Product("Lẵng hoa chúc mừng đối tác", new BigDecimal("980000"), "Lẵng hoa trang nhã gửi tặng đối tác, kèm băng rôn chúc mừng.", "lang-hoa-chuc-mung.jpg", hoaKhaiTruong));
            products.add(new Product("Kệ mini khai trương - tiết kiệm", new BigDecimal("750000"), "Phiên bản kệ nhỏ gọn, chi phí hợp lý cho cửa hàng vừa mở.", "ke-mini-khai-truong.jpg", hoaKhaiTruong));

            products.add(new Product("Vòng hoa chia buồn trang nhã", new BigDecimal("1300000"), "Vòng hoa trắng tím trang trọng, phù hợp lễ tưởng niệm.", "vong-hoa-chia-buon-trang-nha.jpg", hoaChiaBuon));
            products.add(new Product("Kệ hoa tưởng niệm", new BigDecimal("1650000"), "Kệ hoa tông trắng vàng nhạt, thể hiện lòng thành kính.", "ke-hoa-tuong-niem.jpg", hoaChiaBuon));
            products.add(new Product("Vòng hoa lan trắng - trang trọng", new BigDecimal("1950000"), "Vòng hoa lan trắng cao cấp, dành cho lễ tưởng niệm trang trọng.", "vong-hoa-lan-trang.jpg", hoaChiaBuon));
            products.add(new Product("Kệ chia buồn đơn giản", new BigDecimal("890000"), "Kệ chia buồn thiết kế tối giản, tôn trọng gia quyến.", "ke-chia-buon-don-gian.jpg", hoaChiaBuon));

            productRepository.saveAll(products);
        };
    }
}
