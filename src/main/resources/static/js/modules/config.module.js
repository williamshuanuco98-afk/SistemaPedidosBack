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

  if (pathInput) pathInput.value = savedPath;
  if (guiasPathInput) guiasPathInput.value = savedGuiasPath;
  if (letrasPathInput) letrasPathInput.value = savedLetrasPath;
  if (subfolderCheck) subfolderCheck.checked = savedSubfolders;
}

export function saveStorageConfig() {
  const pathInput = document.getElementById('pdfFolderPathInput');
  const guiasPathInput = document.getElementById('guiasPdfFolderPathInput');
  const letrasPathInput = document.getElementById('letrasPdfFolderPathInput');
  const subfolderCheck = document.getElementById('pdfSubfolderCheckbox');
  const feedback = document.getElementById('storageConfigFeedback');

  const newPath = (pathInput?.value || '').trim();
  const newGuiasPath = (guiasPathInput?.value || '').trim();
  const newLetrasPath = (letrasPathInput?.value || '').trim();

  if (!newPath || !newGuiasPath || !newLetrasPath) {
    alert('Por favor ingrese rutas de carpeta válidas para pedidos, guías y letras de cambio.');
    return;
  }

  const useSubfolders = subfolderCheck ? subfolderCheck.checked : true;

  localStorage.setItem('inplabel_pdf_storage_path', newPath);
  localStorage.setItem('inplabel_guias_pdf_storage_path', newGuiasPath);
  localStorage.setItem('inplabel_letras_pdf_storage_path', newLetrasPath);
  localStorage.setItem('inplabel_pdf_subfolders', useSubfolders);

  if (feedback) {
    feedback.className = 'alert alert-success d-flex align-items-center py-2 px-3 fs-7 mb-3';
    feedback.innerHTML = `<i class="bi bi-check-circle-fill me-2 fs-5"></i><div><strong>¡Rutas guardadas exitosamente!</strong><br>Pedidos: <code>${newPath}</code><br>Guías: <code>${newGuiasPath}</code><br>Letras: <code>${newLetrasPath}</code></div>`;
    feedback.classList.remove('d-none');

    setTimeout(() => {
      feedback.classList.add('d-none');
    }, 5000);
  }
}
