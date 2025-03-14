import "./globals.css";

export const metadata = {
  title: "Lumos: Search Engine",
  description:
    "A search engine for the web made as a project for the course Advanced Programming Techniques (APT) at Cairo University.",
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={`antialiased`}>{children}</body>
    </html>
  );
}
