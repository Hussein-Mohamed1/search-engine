const {fontFamily} = require('tailwindcss/defaultTheme');

/** @type {import('tailwindcss').Config} */
module.exports = {
    content: ["./app/**/*.{js,ts,jsx,tsx}", "./pages/**/*.{js,ts,jsx,tsx}", "./components/**/*.{js,ts,jsx,tsx}",],
    theme: {
        extend: {
            fontFamily: {
                sans: ['var(--font-nunito)', ...fontFamily.sans],
                heading: ['var(--font-open-sans)', ...fontFamily.sans],
                body: ['var(--font-nunito-sans)', ...fontFamily.sans],
            },
        },
    },
    plugins: [],
};