package interfaz.controllers;

import dao.*;
import entities.*;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import utils.Sesion;
import utils.cls_browseEBAY;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ProductController {

    @FXML private StackPane imageContainer;
    @FXML private Button btnPrev, btnNext, btnSave, btn_link_pro;
    @FXML private Label lblTitle, lblPrice, lblDescrtiption;
    @FXML private Label lblContentDet, lblContSeller, lblContentShip;
    @FXML private Label lblSign, lblSign1, lblSign11;
    @FXML private ScrollPane scrollPane;


    private final List<Image> images = new ArrayList<>();
    private int currentIndex = 0;
    private ImageView currentImageView;
    private MainController mainController;
    private Producto producto;



    @FXML
    public void initialize() {}

    @FXML
    private void onSave(){
        guardarProducto();
    }

    /** 🔹 Carga un producto completo desde memoria (API o galería) */
    public void loadProduct(Producto producto) {
        this.producto = producto;
        images.clear();

        // =====================
        // IMÁGENES
        // =====================
        try {
            if (producto.getImageUrls() != null && !producto.getImageUrls().isEmpty()) {
                for (String url : producto.getImageUrls()) {
                    try {
                        images.add(new Image(url, true));
                    } catch (Exception ex) {
                        System.err.println("⚠️ Error cargando imagen: " + url);
                    }
                }
            } else if (producto.getUrlProduct() != null && !producto.getUrlProduct().isBlank()) {
                images.add(new Image(producto.getUrlProduct(), true));
            } else {
                images.add(new Image(getClass().getResource("/interfaz/recursos/imagen-rota.png").toExternalForm()));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error cargando imágenes: " + e.getMessage());
        }

        // Mostrar la primera imagen
        if (!images.isEmpty()) {
            currentIndex = 0;
            currentImageView = new ImageView(images.get(currentIndex));
            currentImageView.setFitWidth(700);
            currentImageView.setFitHeight(350);
            currentImageView.setPreserveRatio(true);
            imageContainer.getChildren().setAll(currentImageView);
        }

        // =====================
        // DATOS PRINCIPALES
        // =====================
        lblTitle.setText(producto.getName());
        if (producto.getPriceHistory() != null) {
            PriceHistory ph = producto.getPriceHistory();
            lblPrice.setText(ph.getCurrency() + " " + ph.getPrice());
        } else {
            lblPrice.setText("Precio no disponible");
        }

        // =====================
        // DESCRIPCIÓN DEL PRODUCTO
        // =====================
        String descripcion = producto.getShortDescription();

        if (descripcion != null) {
            descripcion = descripcion.replaceAll("<[^>]*>", ""); // Limpia etiquetas HTML
        }

        if (descripcion == null || descripcion.isBlank()) {
            descripcion = "Sin descripción disponible.";
        } else if (descripcion.length() > 500) {
            descripcion = descripcion.substring(0, 500) + "...";
        }

        lblDescrtiption.setWrapText(true);
        lblDescrtiption.setText(descripcion);
        System.out.println("📜 Descripción mostrada en interfaz: " + descripcion);

        // =====================
        // INFORMACIÓN DETALLADA (DETALLES)
        // =====================
        TextFlow detallesFlow = new TextFlow();
        detallesFlow.setLineSpacing(4);
        Font regular = Font.font("Poppins Light", 12);

        // Categoría
        detallesFlow.getChildren().add(new Text("• Categoría: " + producto.getIdCategory().getCategoryPath() + "\n"));

        // Condición
        detallesFlow.getChildren().add(new Text("• Condición: " + producto.getIdCondition().getConditionPath() + "\n"));

        // Disponibilidad
        String disp = (producto.getAvailable() != null && producto.getAvailable() == 1) ? "En stock" : "Agotado";
        detallesFlow.getChildren().add(new Text("• Disponibilidad: " + disp + "\n"));

        // Cupón
        String cupon = (producto.getIdCoupon() != null)
                ? producto.getIdCoupon().getCouponRedemption()
                : "Ninguno";
        detallesFlow.getChildren().add(new Text("• Cupón disponible: " + cupon + "\n"));

        // Devoluciones
        String devol = (producto.getReturns() != null && producto.getReturns() == 1)
                ? "Aceptadas"
                : "No aceptadas";
        detallesFlow.getChildren().add(new Text("• Devoluciones: " + devol + "\n"));

        // Atributos
        if (producto.getAtributos() != null && !producto.getAtributos().isEmpty()) {
            detallesFlow.getChildren().add(new Text("\nAtributos:\n"));
            for (AtributtesProduct attr : producto.getAtributos()) {
                detallesFlow.getChildren().add(new Text("• " + attr.getAtributte() + ": " + attr.getValue() + "\n"));
            }
        }

        lblContentDet.setGraphic(detallesFlow);

        // =====================
        // INFO DEL VENDEDOR
        // =====================
        lblContSeller.setText("• Vendedor: " + producto.getIdSeller().getUsername());

        // =====================
        // DETALLES DE ENVÍO
        // =====================
        TextFlow envioFlow = new TextFlow();
        envioFlow.setLineSpacing(4);

        if (producto.getEnvios() != null && !producto.getEnvios().isEmpty()) {
            for (var envio : producto.getEnvios()) {
                envioFlow.getChildren().add(new Text("• " + envio.getShippingCarrier() +
                        " (" + envio.getType() + ") — USD " +
                        String.format("%.2f", envio.getShippingCost()) + "\n"));
            }
        } else {
            envioFlow.getChildren().add(new Text("No hay información de envío disponible.\n"));
        }

        lblContentShip.setGraphic(envioFlow);
    }



    // ======== CAMBIO DE IMÁGENES ========
    @FXML
    private void showNext() {
        if (images.isEmpty()) return;
        currentIndex = (currentIndex + 1) % images.size();
        changeImage(images.get(currentIndex));
    }

    @FXML
    private void showPrev() {
        if (images.isEmpty()) return;
        currentIndex = (currentIndex - 1 + images.size()) % images.size();
        changeImage(images.get(currentIndex));
    }

    private void changeImage(Image newImage) {
        ImageView nextImageView = new ImageView(newImage);
        nextImageView.setFitWidth(700);
        nextImageView.setFitHeight(350);
        nextImageView.setPreserveRatio(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), currentImageView);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), nextImageView);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        fadeOut.setOnFinished(e -> {
            imageContainer.getChildren().setAll(nextImageView);
            fadeIn.play();
            currentImageView = nextImageView;
        });
        fadeOut.play();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // ======== SECCIONES DESPLEGABLES ========
    @FXML
    private void toggleDetails() {
        toggleSection(lblContentDet, lblSign);
    }

    @FXML
    private void toggleSeller() {
        toggleSection(lblContSeller, lblSign1);
    }

    @FXML
    private void toggleShip() {
        toggleSection(lblContentShip, lblSign11);
    }
    @FXML
    private void onRederict(){
        try {
            Desktop.getDesktop().browse(new URI(producto.getUrlProduct()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSection(Label contentLabel, Label signLabel) {
        boolean visible = contentLabel.isVisible();
        contentLabel.setVisible(!visible);
        contentLabel.setManaged(!visible);
        signLabel.setText(visible ? "+" : "–");
    }

    @FXML
    private void guardarProducto() {
        if (producto == null) {
            System.err.println("Producto no puede ser nulo");
            //mostrarAlerta("Advertencia", "No hay producto cargado para guardar.", Alert.AlertType.WARNING);
            return;
        }

        try {
            // =====================
            // 1️⃣ GUARDAR VENDEDOR
            // =====================

            SellerDAO sellerDAO = new SellerDAO();
            MarketplaceDAO marketplaceDAO = new MarketplaceDAO();

            Marketplace m = marketplaceDAO.findByName("eBay");

            String username = producto.getIdSeller().getUsername();
            int feedbackScore = producto.getIdSeller().getFeedbackScore();
            double feedbackPercentage = producto.getIdSeller().getFeedbackPorcentage();

            // 1. Buscar si el vendedor ya existe
            Seller vendedorPersistido = sellerDAO.findByUsername(username);

            if (vendedorPersistido == null) {
                // 2. Si no existe → crear uno nuevo
                vendedorPersistido = new Seller(username, feedbackScore, feedbackPercentage, m, null);
                sellerDAO.create(vendedorPersistido);
                System.out.println("🆕 Vendedor creado: " + username);
            } else {
                System.out.println("ℹ️ Vendedor ya existe: " + username);
            }

            // 3. Asignar el vendedor persistido al producto
            producto.setIdSeller(vendedorPersistido);


            // =====================
            // 2️⃣ GUARDAR CATEGORÍA
            // =====================
            CategoryProductDAO categoryDAO = new CategoryProductDAO();

            // Obtener los valores de la categoría actual del producto
            Integer idCat = producto.getIdCategory().getIdCategory();
            String nombreCat = producto.getIdCategory().getCategoryPath();

            // Si no existe, se guarda
            if (!categoryDAO.existsByCategoryId(String.valueOf(idCat))) {
                CategoryProduct nuevaCategoria = new CategoryProduct(idCat, nombreCat);
                categoryDAO.create(nuevaCategoria);
                System.out.println("✅ Categoría guardada: " + nombreCat);
            } else {
                System.out.println("ℹ️ Categoría ya existente: " + nombreCat);
            }

            // 🔹 Recuperar la categoría persistida y asignarla al producto
            CategoryProduct categoriaPersistida = categoryDAO.findByCategoryId(String.valueOf(idCat));
            producto.setIdCategory(categoriaPersistida);



            // =====================
            // 3️⃣ GUARDAR CONDICIÓN
            // =====================
            ConditionProductDAO conditionDAO = new ConditionProductDAO();

            Integer idCond = producto.getIdCondition().getIdCondition();
            String nombreCond = producto.getIdCondition().getConditionPath();

            // Verificar por ID
            if (!conditionDAO.existsByConditionId(String.valueOf(idCond))) {
                ConditionProduct nuevaCondicion = new ConditionProduct(idCond, nombreCond);
                conditionDAO.create(nuevaCondicion);
                System.out.println("✅ Condición guardada: " + nombreCond);
            } else {
                System.out.println("ℹ️ Condición ya existente: " + nombreCond);
            }

            // 🔹 Recuperar la condición persistida y asignarla al producto
            ConditionProduct condicionPersistida = conditionDAO.findByConditionId(String.valueOf(idCond));
            producto.setIdCondition(condicionPersistida);



            // =====================
            // 4️⃣ GUARDAR CUPÓN (si existe)
            // =====================
            if (producto.getIdCoupon() != null) {
                CouponProDAO couponDAO = new CouponProDAO();

                String code = producto.getIdCoupon().getCouponRedemption();
                String itemId = producto.getItemId();

                if (!couponDAO.existsByCodeAndItemId(code, itemId)) {
                    CouponPro nuevoCupon = new CouponPro(
                            itemId,
                            code,
                            producto.getIdCoupon().getExpirationAt()
                    );
                    couponDAO.create(nuevoCupon);
                    System.out.println("✅ Cupón guardado: " + code);
                } else {
                    System.out.println("ℹ️ Cupón ya existente: " + code);
                }

                // 🔹 Recuperar el cupón persistido y asignarlo al producto
                CouponPro cuponPersistido = couponDAO.findByCodeAndItemId(code, itemId);
                producto.setIdCoupon(cuponPersistido);
            }



            // =====================
            // 5️⃣ GUARDAR PRODUCTO
            // =====================
            ProductDAO productDAO = new ProductDAO();

            if (!productDAO.exists(producto.getItemId())) {

                // 🔹 Asegurar que las relaciones estén ya persistidas
                Seller vendedor = producto.getIdSeller();
                CategoryProduct categoria = producto.getIdCategory();
                ConditionProduct condicion = producto.getIdCondition();
                CouponPro cupon = producto.getIdCoupon(); // puede ser null

                // 🔹 Crear nuevo objeto limpio con las referencias correctas
                Producto nuevoProducto = new Producto(
                        producto.getItemId(),
                        producto.getName(),
                        vendedor,
                        categoria,
                        condicion,
                        producto.getRatedProduct(),
                        producto.getUrlProduct(),
                        producto.getCreatedAt()
                );

                nuevoProducto.setShortDescription(producto.getShortDescription());
                nuevoProducto.setReturns(producto.getReturns());
                nuevoProducto.setAvailable(producto.getAvailable());
                nuevoProducto.setIdCoupon(cupon);

                productDAO.create(nuevoProducto); // o insertarProducto()
                System.out.println("✅ Producto guardado: " + producto.getName());

            } else {
                System.out.println("ℹ️ El producto ya existe, no se volverá a insertar.");
            }


            // =====================
            // 6️⃣ Guardar IMÁGENES
            // =====================
            if (producto.getImageUrls() != null && !producto.getImageUrls().isEmpty()) {
                ImagesProductDAO imgDAO = new ImagesProductDAO();

                for (String url : producto.getImageUrls()) {
                    ImagesProduct img = new ImagesProduct(producto, url);

                    if (!imgDAO.existeImagen(producto.getItemId(), url)) {
                        imgDAO.create(img); // o insertarImagen() según tu genericDAO
                        System.out.println("✅ Imagen guardada: " + url);
                    } else {
                        System.out.println("ℹ️ Imagen ya registrada: " + url);
                    }
                }
            }


            // =====================
            // 7️⃣ GUARDAR ATRIBUTOS
            // =====================
            if (producto.getAtributos() != null && !producto.getAtributos().isEmpty()) {
                AtributtesProductDAO attrDAO = new AtributtesProductDAO();

                for (AtributtesProduct attr : producto.getAtributos()) {
                    attr.setIdItem(producto); // 🔹 asegúrate de que la relación esté establecida

                    if (!attrDAO.existeAtributo(producto.getItemId(), attr.getAtributte())) {
                        attrDAO.create(attr); // o insertarAtributo(), depende de tu genericDAO
                        System.out.println("✅ Atributo guardado: " + attr.getAtributte());
                    } else {
                        System.out.println("ℹ️ Atributo ya existente: " + attr.getAtributte());
                    }
                }
            }

            // =====================
            // 8️⃣ Guardar OPCIONES DE ENVÍO
            // =====================
            if (producto.getEnvios() != null && !producto.getEnvios().isEmpty()) {
                ShippingProductDAO shipDAO = new ShippingProductDAO();

                for (ShippingProduct envio : producto.getEnvios()) {
                    envio.setItem(producto); // 🔹 asegúrate de usar setItem(), no setProducto()

                    if (!shipDAO.existeEnvio(producto.getItemId(), envio.getShippingCarrier())) {
                        shipDAO.create(envio); // o insertarEnvio() según tu genericDAO
                        System.out.println("✅ Envío guardado: " + envio.getShippingCarrier());
                    } else {
                        System.out.println("ℹ️ Envío ya existente: " + envio.getShippingCarrier());
                    }
                }
            }

            // =====================
            // 9️⃣ Guardar HISTORIAL DE PRECIO
            // =====================
            if (producto.getPriceHistory() != null) {
                PriceHistoryDAO priceDAO = new PriceHistoryDAO();
                var ph = producto.getPriceHistory();

                if (!priceDAO.existeHistorial(ph.getItemId(), ph.getRecordedAt())) {
                    priceDAO.create(ph); // o insertarHistorial() según tu genericDAO
                    System.out.println("✅ Historial de precio guardado: " + ph.getPrice() + " " + ph.getCurrency());
                } else {
                    System.out.println("ℹ️ El historial de precio ya existe para esta fecha.");
                }
            }

            // =====================
            // 🔟 GUARDAR EN WISHLIST
            // =====================
            try {
                WishlistDAO wishlistDAO = new WishlistDAO();
                Auth usuario = Sesion.getUsuario();       // usuario logueado
                // Obtener el Producto ya persistido (IMPORTANTE)
                Producto productoBD = productDAO.read(producto.getItemId());

                if (productoBD == null) {
                    System.err.println("❌ No se pudo guardar en wishlist: producto no encontrado en BD.");
                    return;
                }

                // Validar que no exista
                boolean existe = wishlistDAO.existsWishlist(usuario.getId(), productoBD.getItemId());
                if (!existe) {
                    WishlistProduct nuevo = new WishlistProduct();
                    nuevo.setIdUser(usuario);
                    nuevo.setIdItem(productoBD);

                    wishlistDAO.create(nuevo);
                    System.out.println("💚 Wishlist: producto añadido correctamente.");
                } else {
                    System.out.println("💛 Wishlist: este producto ya estaba agregado.");
                }

            } catch (Exception ex) {
                System.err.println("❌ Error guardando en wishlist: " + ex.getMessage());
            }



            //mostrarAlerta("Éxito", "Producto guardado correctamente con todas sus relaciones.", Alert.AlertType.INFORMATION);
            System.out.println("✅ Producto completo guardado: " + producto.getName());

        } catch (Exception e) {
            e.printStackTrace();
            //mostrarAlerta("Error", "Error al guardar producto: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

}
