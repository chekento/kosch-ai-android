(() => {
  const preferred = (navigator.language || "").toLowerCase();
  let active = preferred.startsWith("de") ? "de" : "en";

  const apply = (lang) => {
    active = lang === "de" ? "de" : "en";
    document.documentElement.lang = active;
    document.querySelectorAll("[data-lang]").forEach((node) => {
      node.hidden = node.dataset.lang !== active;
    });
    document.querySelectorAll("[data-set-lang]").forEach((button) => {
      button.setAttribute("aria-pressed", button.dataset.setLang === active ? "true" : "false");
    });
  };

  document.querySelectorAll("[data-set-lang]").forEach((button) => {
    button.addEventListener("click", () => apply(button.dataset.setLang));
  });
  apply(active);
})();
