import {SearchBar} from "@/app/components/searchBar";


export default function Home() {
    return (<main className="flex items-center justify-center h-screen">
        <div className="flex flex-col w-screen m-auto text-center items-center space-y-6">

            {/* Lumos Logo */}
            <div className="flex flex-row text-5xl font-extrabold font-mono text-center items-center">
                <div className={"tracking-widest"}>Lumos</div>
                <div className="text-4xl">💡</div>
            </div>


            {/* Search Bar */}
            <SearchBar />

            {/* Search Buttons */}
            <div className="flex flex-row  space-x-4 text-xs">
                <button className="px-4 py-2 rounded-md bg-gray-200 text-black font-bold hover:bg-gray-300">
                    Search
                </button>
                <button className="px-4 py-2 rounded-md bg-gray-200 text-black font-bold hover:bg-gray-300">
                    I&#39;m Feeling Lucky
                </button>
            </div>
        </div>
    </main>);
}
