export const themeManager = {
  currentTheme: 'dark',

  initTheme() {
    const savedTheme = localStorage.getItem('inplabel_theme') || 'dark';
    // Preload logo images to avoid white flash on theme switch
    const imgDark = new Image();
    imgDark.src = 'img/inplabel-logo-dark.png';
    const imgLight = new Image();
    imgLight.src = 'img/inplabel-logo.png';

    this.setTheme(savedTheme);
  },

  setTheme(theme) {
    this.currentTheme = theme;
    
    // Disable CSS transitions temporarily to prevent color transition delay/lag
    document.documentElement.classList.add('no-transitions');

    // Set both data-theme and Bootstrap's native data-bs-theme synchronously
    document.documentElement.setAttribute('data-theme', theme);
    document.documentElement.setAttribute('data-bs-theme', theme);
    document.body.setAttribute('data-theme', theme);
    document.body.setAttribute('data-bs-theme', theme);
    localStorage.setItem('inplabel_theme', theme);

    const logoImg = document.getElementById('brandLogoImg');
    const themeLabel = document.getElementById('themeToggleLabel');

    if (theme === 'dark') {
      if (logoImg) logoImg.src = 'img/inplabel-logo-dark.png';
      if (themeLabel) themeLabel.textContent = 'Modo Oscuro';
    } else {
      if (logoImg) logoImg.src = 'img/inplabel-logo.png';
      if (themeLabel) themeLabel.textContent = 'Modo Claro';
    }

    // Force synchronous layout paint before re-enabling transitions
    void document.documentElement.offsetWidth;

    requestAnimationFrame(() => {
      document.documentElement.classList.remove('no-transitions');
      if (typeof window.updateChartThemes === 'function') {
        try {
          window.updateChartThemes();
        } catch (e) {}
      }
    });
  },

  toggleTheme() {
    const nextTheme = this.currentTheme === 'dark' ? 'light' : 'dark';
    this.setTheme(nextTheme);
  }
};
