const DEFAULT_STORAGE_PATH = 'C:\\Users\\User\\OneDrive\\Escritorio\\OrdenesI';
const DEFAULT_GUIAS_STORAGE_PATH = 'C:\\Users\\User\\OneDrive\\Escritorio\\GuiasI';
const DEFAULT_LETRAS_STORAGE_PATH = 'C:\\Users\\User\\OneDrive\\Escritorio\\LetrasI';

export function renderConfigView(clientsCount = 0, productsCount = 0) {
  const bdClients = document.getElementById('bdClientsCount');
  const bdProducts = document.getElementById('bdProductsCount');

  if (bdClients) bdClients.textContent = `${clientsCount} Clientes`;
  if (bdProducts) bdProducts.textContent = `${productsCount} Productos`;

  // Load saved storage path configuration
  const savedPath = localStorage.getItem('inplabel_pdf_storage_path') || DEFAULT_STORAGE_PATH;
  const savedGuiasPath = localStorage.getItem('inplabel_guias_pdf_storage_path') || DEFAULT_GUIAS_STORAGE_PATH;
  const savedLetrasPath = localStorage.getItem('inplabel_letras_pdf_storage_path') || DEFAULT_LETRAS_STORAGE_PATH;
  const savedSubfolders = localStorage.getItem('inplabel_pdf_subfolders') === 'true';

  const pathInput = document.getElementById('pdfFolderPathInput');
  const guiasPathInput = document.getElementById('guiasPdfFolderPathInput');
  const letrasPathInput = document.getElementById('letrasPdfFolderPathInput');
  const subfolderCheck = document.getElementById('pdfSubfolderCheckbox');

  if (pathInput) { pathInput.value = 'Servidor: APP_STORAGE_ROOT/Pedidos'; pathInput.readOnly = true; }
  if (guiasPathInput) { guiasPathInput.value = 'Servidor: APP_STORAGE_ROOT/Guias'; guiasPathInput.readOnly = true; }
  if (letrasPathInput) { letrasPathInput.value = 'Servidor: APP_STORAGE_ROOT/Letras'; letrasPathInput.readOnly = true; }
  if (subfolderCheck) subfolderCheck.checked = savedSubfolders;
}

export function saveStorageConfig() {
  localStorage.setItem('inplabel_pdf_subfolders', String(document.getElementById('pdfSubfolderCheckbox')?.checked !== false));
  alert('Por seguridad, la carpeta de documentos se configura en el servidor mediante APP_STORAGE_ROOT. Los archivos se organizan en Pedidos, Guias y Letras.');
}
