package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ProductEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.NexusViewModel
import com.example.util.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: NexusViewModel) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }
    var selectedStockFilter by remember { mutableStateOf("Todos") } // "Todos", "Disponible", "Bajo", "Agotado"

    var showProductModal by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showCategoryModal by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategoryFilter, selectedStockFilter) {
        products.filter { p ->
            val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) || p.code.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategoryFilter == "Todas" || p.category == selectedCategoryFilter
            val matchesStock = when (selectedStockFilter) {
                "Disponible" -> p.stock > p.minStock
                "Bajo" -> p.stock in 1..p.minStock
                "Agotado" -> p.stock <= 0
                else -> true
            }
            matchesSearch && matchesCat && matchesStock
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showCategoryModal = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = NexusWhite,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = "Categorías")
                }
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductModal = true
                    },
                    containerColor = NexusDark,
                    contentColor = NexusWhite
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre o código...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- FILTERS ROW ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Filter Chip
                var catDropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    FilterChip(
                        selected = selectedCategoryFilter != "Todas",
                        onClick = { catDropdownExpanded = true },
                        label = { Text("Cat: $selectedCategoryFilter", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = {
                                selectedCategoryFilter = "Todas"
                                catDropdownExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryFilter = cat.name
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Stock Filter Chip
                var stockDropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    FilterChip(
                        selected = selectedStockFilter != "Todos",
                        onClick = { stockDropdownExpanded = true },
                        label = { Text("Stock: $selectedStockFilter") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = stockDropdownExpanded,
                        onDismissRequest = { stockDropdownExpanded = false }
                    ) {
                        listOf("Todos", "Disponible", "Bajo", "Agotado").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedStockFilter = opt
                                    stockDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Mostrando ${filteredProducts.size} de ${products.size} productos",
                style = MaterialTheme.typography.labelMedium.copy(color = NexusTextSecondary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- PRODUCTS LIST ---
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(56.dp), tint = NexusTextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No se encontraron productos", style = MaterialTheme.typography.bodyMedium.copy(color = NexusTextSecondary))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            onEdit = {
                                editingProduct = product
                                showProductModal = true
                            },
                            onDelete = {
                                viewModel.deleteProduct(product)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- PRODUCT FORM MODAL ---
    if (showProductModal) {
        ProductFormDialog(
            product = editingProduct,
            categories = categories,
            suppliers = suppliers,
            onDismiss = { showProductModal = false },
            onSave = { savedProd ->
                viewModel.saveProduct(savedProd)
                showProductModal = false
            }
        )
    }

    // --- CATEGORIES MODAL ---
    if (showCategoryModal) {
        CategoryManageDialog(
            categories = categories,
            onAddCategory = { viewModel.addCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { showCategoryModal = false }
        )
    }
}

@Composable
private fun ProductItemCard(
    product: ProductEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock in 1..product.minStock
    val profit = product.price - product.cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Cód: ${product.code} | Cat: ${product.category}",
                        style = MaterialTheme.typography.labelSmall.copy(color = NexusTextSecondary)
                    )
                }
                when {
                    isOutOfStock -> StatusBadge(text = "AGOTADO", type = "danger")
                    isLowStock -> StatusBadge(text = "STOCK BAJO (${product.stock})", type = "warning")
                    else -> StatusBadge(text = "DISPONIBLE (${product.stock})", type = "disponible")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = NexusBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Costo: ${Utils.formatCurrency(product.cost)}", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextSecondary))
                    Text("Precio: ${Utils.formatCurrency(product.price)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Ganancia: ${Utils.formatCurrency(profit)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = NexusGreen))
                    Text("Stock mín: ${product.minStock}", style = MaterialTheme.typography.bodySmall.copy(color = NexusTextSecondary))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = NexusRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
private fun ProductFormDialog(
    product: ProductEntity?,
    categories: List<CategoryEntity>,
    suppliers: List<com.example.data.model.SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var code by remember { mutableStateOf(product?.code ?: "") }
    var category by remember { mutableStateOf(product?.category ?: if (categories.isNotEmpty()) categories.first().name else "General") }
    var supplier by remember { mutableStateOf(product?.supplier ?: if (suppliers.isNotEmpty()) suppliers.first().name else "General") }
    var costStr by remember { mutableStateOf(product?.cost?.toString() ?: "0.0") }
    var priceStr by remember { mutableStateOf(product?.price?.toString() ?: "0.0") }
    var stockStr by remember { mutableStateOf(product?.stock?.toString() ?: "10") }
    var minStockStr by remember { mutableStateOf(product?.minStock?.toString() ?: "5") }
    var description by remember { mutableStateOf(product?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Nuevo Producto" else "Editar Producto", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Producto *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código de Barras / Clave *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoría") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        label = { Text("Proveedor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costStr,
                            onValueChange = { costStr = it },
                            label = { Text("Precio Compra ($)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Precio Venta ($)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Stock Actual") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minStockStr,
                            onValueChange = { minStockStr = it },
                            label = { Text("Stock Mínimo") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costStr.toDoubleOrNull() ?: 0.0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockStr.toIntOrNull() ?: 0
                    val minStock = minStockStr.toIntOrNull() ?: 0

                    if (name.isBlank() || code.isBlank()) return@Button

                    val newProd = ProductEntity(
                        id = product?.id ?: 0L,
                        name = name.trim(),
                        code = code.trim(),
                        category = category.trim(),
                        supplier = supplier.trim(),
                        cost = cost,
                        price = price,
                        stock = stock,
                        minStock = minStock,
                        description = description.trim(),
                        profit = price - cost
                    )
                    onSave(newProd)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexusDark)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CategoryManageDialog(
    categories: List<CategoryEntity>,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var newCatName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestión de Categorías", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = { Text("Nueva categoría...") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCatName.isNotBlank()) {
                                onAddCategory(newCatName)
                                newCatName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                    }
                }

                Divider()

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexusRed)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
